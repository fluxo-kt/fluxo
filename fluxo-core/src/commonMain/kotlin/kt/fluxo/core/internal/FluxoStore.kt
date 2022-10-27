package kt.fluxo.core.internal

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kt.fluxo.core.Bootstrapper
import kt.fluxo.core.IntentFilter
import kt.fluxo.core.IntentHandler
import kt.fluxo.core.Store
import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.debug.DEBUG
import kt.fluxo.core.debug.debugIntentWrapper
import kt.fluxo.core.dsl.InputStrategyScope
import kt.fluxo.core.dsl.SideJobScope.RestartState
import kt.fluxo.core.intercept.FluxoEvent
import kt.fluxo.core.intercept.StoreRequest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@PublishedApi
@InternalFluxoApi
@Suppress("TooManyFunctions", "TooGenericExceptionCaught")
internal class FluxoStore<Intent, State, SideEffect : Any>(
    initialState: State,
    private val intentHandler: IntentHandler<Intent, State, SideEffect>,
    private val conf: FluxoConf<Intent, State, SideEffect>,
) : Store<Intent, State, SideEffect> {

    private companion object {
        private const val F = "Fluxo"
    }

    override val name: String get() = conf.name

    private val scope: CoroutineScope
    private val intentContext: CoroutineContext
    private val sideJobScope: CoroutineScope
    private val interceptorScope: CoroutineScope
    private val requestsChannel: Channel<StoreRequest<Intent, State>> = conf.inputStrategy.createQueue()

    private val sideJobsChannel = Channel<SideJobRequest<Intent, State, SideEffect>>(Channel.BUFFERED, BufferOverflow.SUSPEND)
    private val sideJobsMap = ConcurrentHashMap<String, RunningSideJob<Intent, State, SideEffect>>()

    private val mutableState = MutableStateFlow(initialState)
    override val stateFlow: StateFlow<State> get() = mutableState

    private val sideEffectChannel: Channel<SideEffect>
    override val sideEffectFlow: Flow<SideEffect>

    private val events = MutableSharedFlow<FluxoEvent<Intent, State, SideEffect>>(extraBufferCapacity = 256)

    override val isActive: Boolean
        get() = scope.isActive

    private val initialised = atomic(false)

    @OptIn(InternalCoroutinesApi::class)
    private val cancellationCause get() = scope.coroutineContext[Job]?.getCancellationException()

    init {
        val ctx = conf.eventLoopContext + when {
            !conf.debugChecks -> EmptyCoroutineContext
            else -> CoroutineName(toString())
        }

        val parent = ctx[Job]
        val job = if (conf.closeOnExceptions) Job(parent) else SupervisorJob(parent)
        job.invokeOnCompletion(::onShutdown)

        val parentExceptionHandler = conf.exceptionHandler ?: ctx[CoroutineExceptionHandler]
        val exceptionHandler = CoroutineExceptionHandler { context, e ->
            events.tryEmit(FluxoEvent.UnhandledError(this, e))
            if (parentExceptionHandler != null) {
                parentExceptionHandler.handleException(context, e)
            } else {
                throw e
            }
        }
        scope = CoroutineScope(ctx + exceptionHandler + job)
        intentContext = scope.coroutineContext + conf.intentContext + when {
            !conf.debugChecks -> EmptyCoroutineContext
            else -> CoroutineName("$F[$name:intentScope]")
        }
        sideJobScope = scope + conf.sideJobsContext + exceptionHandler + SupervisorJob(job) + when {
            !conf.debugChecks -> EmptyCoroutineContext
            else -> CoroutineName("$F[$name:sideJobScope]")
        }
        // Shouldn't close immediately with scope
        interceptorScope = scope + conf.interceptorContext + SupervisorJob() + when {
            !conf.debugChecks -> EmptyCoroutineContext
            else -> CoroutineName("$F[$name:interceptorScope]")
        }

        sideEffectChannel = Channel(conf.sideEffectBufferSize, BufferOverflow.SUSPEND) {
            scope.launch(Dispatchers.Unconfined) {
                events.emit(FluxoEvent.SideEffectDropped(this@FluxoStore, it))
            }
        }
        sideEffectFlow = sideEffectChannel.receiveAsFlow().let {
            // Wrap Flow for lazy initialization
            if (!conf.lazy) it else flow {
                start()
                emitAll(it)
            }
        }


        if (!conf.lazy) {
            // eager start
            start()
        } else {
            // lazy start on state subscription
            scope.launch {
                var previous = 0
                mutableState.subscriptionCount.collect { v ->
                    if (v != previous) {
                        if (previous <= 0 && v > 0) {
                            start()
                            cancel()
                        }
                        previous = v
                    }
                }
            }
        }
    }


    override fun start() {
        if (!isActive) {
            throw StoreClosedException("Store is closed, it cannot be restarted", cancellationCause?.let { it.cause ?: it })
        }
        if (initialised.compareAndSet(expect = false, update = true)) {
            launch()
        }
    }

    override fun close() {
        scope.cancel()
    }

    override suspend fun sendAsync(intent: Intent): Deferred<Unit> {
        start()
        val i = if (DEBUG || conf.debugChecks) debugIntentWrapper(intent) else intent
        events.emit(FluxoEvent.IntentQueued(this, i))
        val deferred = CompletableDeferred<Unit>()
        requestsChannel.send(StoreRequest.HandleIntent(deferred, i))
        return deferred
    }

    override fun send(intent: Intent) {
        start()
        val i = if (DEBUG || conf.debugChecks) debugIntentWrapper(intent) else intent
        scope.launch(Dispatchers.Unconfined) {
            @Suppress("DeferredResultUnused") sendAsync(i)
        }
    }


    override fun toString(): String = "$F[$name]"


    // region Internals

    private suspend fun updateState(nextValue: State): State {
        while (true) {
            val prevValue = mutableState.value
            if (prevValue == nextValue) {
                return prevValue
            }
            if (mutableState.compareAndSet(prevValue, nextValue)) {
                // event fired only if state changed
                events.emit(FluxoEvent.StateChanged(this, nextValue))
                return nextValue
            }
        }
    }

    private suspend fun updateStateAndGet(function: (State) -> State): State {
        while (true) {
            val prevValue = mutableState.value
            val nextValue = function(prevValue)
            if (mutableState.compareAndSet(prevValue, nextValue)) {
                if (prevValue != nextValue) {
                    // event fired only if state changed
                    events.emit(FluxoEvent.StateChanged(this, nextValue))
                }
                return nextValue
            }
        }
    }

    private suspend fun postSideEffect(sideEffect: SideEffect) {
        sideEffectChannel.send(sideEffect)
        events.emit(FluxoEvent.SideEffectQueued(this@FluxoStore, sideEffect))
    }

    private suspend fun postSideJob(jobRequest: SideJobRequest<Intent, State, SideEffect>) {
        sideJobsChannel.send(jobRequest)
        events.emit(FluxoEvent.SideJobQueued(this@FluxoStore, jobRequest.key))
    }

    /** Will be called only once for each [FluxoStore] */
    private fun launch() = scope.launch {
        // observe and process intents
        val filteredIntentsFlow = requestsChannel.receiveAsFlow().run {
            when (conf.intentFilter) {
                null -> this
                else -> filter(::filterRequest).flowOn(intentContext.minusKey(Job))
            }
        }
        val inputStrategy = conf.inputStrategy
        val inputStrategyScope: InputStrategyScope<Intent, State> = { request ->
            when (request) {
                is StoreRequest.HandleIntent -> {
                    if (conf.debugChecks) {
                        withContext(CoroutineName("$F[$name <= Intent ${request.intent}]")) {
                            safelyHandleIntent(request.intent, request.deferred)
                        }
                    } else {
                        safelyHandleIntent(request.intent, request.deferred)
                    }
                }

                is StoreRequest.RestoreState -> {
                    updateState(request.state)
                    request.deferred?.complete(Unit)
                }
            }
        }
        scope.launch(intentContext) {
            with(inputStrategy) {
                inputStrategyScope.processRequests(filteredQueue = filteredIntentsFlow)
            }
        }

        // start sideJobs
        sideJobScope.launch {
            for (sideJobRequest in sideJobsChannel) {
                safelyStartSideJob(sideJobRequest)
            }
        }

        // launch interceptors handling (stop on StoreClosed)
        if (conf.interceptors.isNotEmpty()) {
            val interceptorScope = FluxoInterceptorScopeImpl(
                storeName = name,
                scope = interceptorScope,
                sendRequest = requestsChannel::send,
            )
            val eventsFlow: Flow<FluxoEvent<Intent, State, SideEffect>> = events.transformWhile {
                emit(it)
                it !is FluxoEvent.StoreClosed
            }
            for (interceptor in conf.interceptors) {
                with(interceptor) {
                    try {
                        interceptorScope.start(eventsFlow)
                    } catch (e: Throwable) {
                        events.emit(FluxoEvent.UnhandledError(this@FluxoStore, e))
                        conf.exceptionHandler?.handleException(currentCoroutineContext(), e)
                    }
                }
            }
        }
        events.emit(FluxoEvent.StoreStarted(this@FluxoStore))

        // bootstrap
        conf.bootstrapper?.let { bootstrapper ->
            safelyRunBootstrapper(bootstrapper)
        }
    }

    /**
     * Filters flow of [StoreRequest.HandleIntent]. Will be called only if [IntentFilter] available.
     */
    private suspend fun filterRequest(request: StoreRequest<Intent, State>): Boolean {
        when (request) {
            is StoreRequest.RestoreState -> {
                // when restoring state, always accept the item
                return true
            }

            is StoreRequest.HandleIntent -> {
                // when handling an Intent, check with the IntentFilter to see if it should be accepted.
                val currentState = mutableState.value
                val shouldAcceptIntent = conf.intentFilter?.invoke(currentState, request.intent) ?: true

                if (!shouldAcceptIntent) {
                    events.emit(FluxoEvent.IntentRejected(this, currentState, request.intent))
                    request.deferred?.complete(Unit)
                }

                return shouldAcceptIntent
            }
        }
    }

    private suspend fun safelyHandleIntent(intent: Intent, deferred: CompletableDeferred<Unit>?) {
        events.emit(FluxoEvent.IntentAccepted(this, intent))

        val stateBeforeCancellation = mutableState.value
        try {
            val handlerScope = StoreScopeImpl(
                job = intent,
                guardian = when {
                    !conf.debugChecks -> null
                    else -> InputStrategyGuardian(parallelProcessing = conf.inputStrategy.parallelProcessing)
                },
                getState = mutableState::value,
                updateStateAndGet = ::updateStateAndGet,
                sendSideEffect = ::postSideEffect,
                sendSideJob = ::postSideJob,
            )
            with(handlerScope) {
                with(intentHandler) {
                    handleIntent(intent)
                }
            }
            handlerScope.close()
            events.emit(FluxoEvent.IntentHandledSuccessfully(this@FluxoStore, intent))
        } catch (_: CancellationException) {
            // reset state as intent did not complete successfully
            if (conf.inputStrategy.rollbackOnCancellation) {
                updateState(stateBeforeCancellation)
            }
            events.emit(FluxoEvent.IntentCancelled(this, intent))
        } catch (e: Throwable) {
            events.emit(FluxoEvent.IntentHandlerError(this, intent, e))
            if (!conf.closeOnExceptions) {
                conf.exceptionHandler?.handleException(currentCoroutineContext(), e)
            } else {
                throw e
            }
        } finally {
            deferred?.complete(Unit)
        }
    }

    private suspend fun safelyStartSideJob(request: SideJobRequest<Intent, State, SideEffect>) {
        val key = request.key
        val block = request.block

        val map = sideJobsMap
        val prevJob = map[key]
        val restartState = if (prevJob != null) RestartState.Restarted else RestartState.Initial

        // Cancel if we have a sideJob already running
        prevJob?.run {
            job?.cancel()
            job = null
        }

        // Go through and remove any sideJobs that have completed (either by
        // cancellation or because they finished on their own)
        map.entries.filterNot { it.value.job?.isActive == true }.map { it.key }.forEach { map.remove(it) }

        // Launch a new sideJob in its own isolated coroutine scope where:
        //   1) cancelled when the scope or sideJobScope cancelled
        //   2) errors caught for crash reporting
        //   3) has a supervisor job, so we can cancel the sideJob without cancelling whole store scope
        //
        // Consumers of this sideJob can launch many jobs, and all will be cancelled together when the
        // sideJob restarted, or the store scope cancelled.
        map[key] = RunningSideJob(
            request = request,
            job = sideJobScope.launch(
                context = when {
                    !conf.debugChecks -> EmptyCoroutineContext
                    else -> CoroutineName("$F[$name SideJob $key <= Intent ${request.parent}]")
                }
            ) {
                events.emit(FluxoEvent.SideJobStarted(this@FluxoStore, key, restartState))
                try {
                    SideJobScopeImpl(
                        sendIntent = ::sendAsync,
                        sendSideEffect = ::postSideEffect,
                        currentStateWhenStarted = mutableState.value,
                        restartState = restartState,
                        coroutineScope = this,
                    ).block()
                    events.emit(FluxoEvent.SideJobCompleted(this@FluxoStore, key, restartState))
                } catch (_: CancellationException) {
                    events.emit(FluxoEvent.SideJobCancelled(this@FluxoStore, key, restartState))
                } catch (e: Throwable) {
                    events.emit(FluxoEvent.SideJobError(this@FluxoStore, key, restartState, e))
                    if (!conf.closeOnExceptions) {
                        conf.exceptionHandler?.handleException(currentCoroutineContext(), e)
                    } else {
                        throw e
                    }
                }
            }
        )
    }

    private suspend fun safelyRunBootstrapper(bootstrapper: Bootstrapper<Intent, State, SideEffect>) = withContext(intentContext) {
        events.emit(FluxoEvent.BootstrapperStart(this@FluxoStore, bootstrapper))
        val bootstrapperScope = BootstrapperScopeImpl(
            bootstrapper = bootstrapper,
            guardian = if (conf.debugChecks) InputStrategyGuardian(parallelProcessing = false) else null,
            getState = mutableState::value,
            updateStateAndGet = ::updateStateAndGet,
            sendIntent = ::sendAsync,
            sendSideEffect = ::postSideEffect,
            sendSideJob = ::postSideJob,
        )
        try {
            bootstrapperScope.bootstrapper()
            bootstrapperScope.close()
            events.emit(FluxoEvent.BootstrapperFinished(this@FluxoStore, bootstrapper))
        } catch (e: Throwable) {
            events.emit(FluxoEvent.BootstrapperError(this@FluxoStore, bootstrapper, e))
            if (!conf.closeOnExceptions) {
                conf.exceptionHandler?.handleException(currentCoroutineContext(), e)
            } else {
                throw e
            }
        }
    }

    /**
     * Called on [scope] completion for any reason.
     */
    private fun onShutdown(cause: Throwable?) {
        events.tryEmit(FluxoEvent.StoreClosed(this, cause))

        val ce = cause.toCancellationException()
        for (value in sideJobsMap.values) {
            value.job?.cancel(ce)
            value.job = null
        }
        sideJobsMap.clear()

        requestsChannel.close(ce)
        sideEffectChannel.close(ce)
        sideJobsChannel.close(ce)

        interceptorScope.launch {
            @Suppress("MagicNumber") delay(5_000)
            interceptorScope.cancel(cancellationCause)
        }
    }

    // endregion
}
