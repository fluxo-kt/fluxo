package kt.fluxo.core.internal

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.IntentHandler
import kt.fluxo.core.Store
import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.debug.debugIntentWrapper
import kt.fluxo.core.dsl.InputStrategyScope
import kt.fluxo.core.dsl.SideJobScope
import kt.fluxo.core.dsl.SideJobScope.RestartState
import kt.fluxo.core.intercept.FluxoEvent
import kt.fluxo.core.intercept.StoreRequest
import kotlin.coroutines.CoroutineContext

@PublishedApi
@InternalFluxoApi
@Suppress("TooManyFunctions")
internal class FluxoStore<Intent, State, SideEffect : Any>(
    initialState: State,
    private val intentHandler: IntentHandler<Intent, State, SideEffect>,
    private val conf: FluxoConf<Intent, State, SideEffect>,
) : Store<Intent, State, SideEffect> {

    override val name: String get() = conf.name

    private val scope: CoroutineScope
    private val intentContext: CoroutineContext
    private val sideJobScope: CoroutineScope
    private val dispatchChannel: Channel<StoreRequest<Intent, State>> = conf.inputStrategy.createQueue()

    private val sideJobsChannel = Channel<SideJobRequest<Intent, State, SideEffect>>(Channel.BUFFERED, BufferOverflow.SUSPEND)

    private val sideJobsMap = ConcurrentHashMap<String, RunningSideJob<Intent, State, SideEffect>>()

    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<State>
        get() {
            start()
            return mutableState
        }

    private val sideEffectChannel = Channel<SideEffect>(conf.sideEffectBufferSize)
    override val sideEffectFlow: Flow<SideEffect> = sideEffectChannel.receiveAsFlow()
        get() {
            start()
            return field
        }

    private val notifications = MutableSharedFlow<FluxoEvent<Intent, State, SideEffect>>(extraBufferCapacity = 256)

    private val initialised = atomic(false)

    init {
        val ctx = conf.eventLoopContext

        val parent = ctx[Job]
        val job = if (conf.closeOnExceptions) Job(parent) else SupervisorJob(parent)
        job.invokeOnCompletion(::onShutdown)

        val exceptionHandler = CoroutineExceptionHandler { context, e ->
            notifications.tryEmit(FluxoEvent.UnhandledError(this, e))
            conf.exceptionHandler?.handleException(context, e)
        }
        scope = CoroutineScope(ctx + exceptionHandler + job)
        intentContext = ctx + conf.intentContext + exceptionHandler + SupervisorJob(job)
        sideJobScope = scope + conf.sideJobsContext + exceptionHandler + SupervisorJob(job)

        // eager start
        if (!conf.lazy) {
            start()
        }
    }


    override fun start() {
        check(scope.isActive) { "Store is closed, it cannot be restarted" }
        if (initialised.compareAndSet(expect = false, update = true)) {
            launch()
        }
    }

    override fun stop() {
        scope.cancel()
    }

    override suspend fun sendAsync(intent: Intent): Deferred<Unit> {
        start()
        notifications.emit(FluxoEvent.IntentQueued(this, intent))
        val completionDeferred = CompletableDeferred<Unit>()
        dispatchChannel.send(StoreRequest.HandleIntent(completionDeferred, intent))
        return completionDeferred
    }

    override fun send(intent: Intent): ChannelResult<Unit> {
        start()
        val intent0 = debugIntentWrapper(intent) ?: intent
        notifications.tryEmit(FluxoEvent.IntentQueued(this, intent0))
        val result = dispatchChannel.trySend(StoreRequest.HandleIntent(null, intent0))
        if (result.isFailure || result.isClosed) {
            // TODO: Is it possible at all?
            notifications.tryEmit(FluxoEvent.IntentDropped(this, intent0))
        }
        return result
    }


    // region Internals

    private suspend fun updateState(nextValue: State): State {
        while (true) {
            val prevValue = mutableState.value
            if (!mutableState.compareAndSet(prevValue, nextValue)) {
                continue
            }
            if (prevValue != nextValue) {
                // event is fired only if state changed
                notifications.emit(FluxoEvent.StateChanged(this, nextValue))
            }
            return nextValue
        }
    }

    private fun updateStateAndGet(function: (State) -> State): State {
        while (true) {
            val prevValue = mutableState.value
            val nextValue = function(prevValue)
            if (!mutableState.compareAndSet(prevValue, nextValue)) {
                continue
            }
            if (prevValue != nextValue) {
                // event is fired only if state changed
                notifications.tryEmit(FluxoEvent.StateChanged(this, nextValue))
            }
            return nextValue
        }
    }

    private suspend fun postSideEffect(sideEffect: SideEffect) {
        sideEffectChannel.send(sideEffect)
        notifications.emit(FluxoEvent.SideEffectQueued(this@FluxoStore, sideEffect))
    }

    private fun launch() {
        // observe and process intents
        // TODO: Switch to channel?
        val filteredIntentsFlow = dispatchChannel
            .receiveAsFlow()
            .filter(::filterRequest)
            .flowOn(intentContext)
        val inputStrategy = conf.inputStrategy
        val inputStrategyScope: InputStrategyScope<Intent, State> = { request, guardian ->
            when (request) {
                is StoreRequest.HandleIntent -> {
                    safelyHandleIntent(request.intent, request.deferred, guardian)
                }

                is StoreRequest.RestoreState -> {
                    updateState(request.state)
                    request.deferred?.complete(Unit)
                }
            }
        }
        scope.launch(intentContext) {
            with(inputStrategy) {
                inputStrategyScope.processInputs(filteredQueue = filteredIntentsFlow)
            }
        }

        // start sideJobs
        sideJobScope.launch {
            for (it in sideJobsChannel) {
                safelyStartSideJob(it.key, it.block)
            }
        }

        // launch interceptors handling
        val interceptorScope = FluxoInterceptorScopeImpl(
            storeName = name,
            storeScope = scope + conf.interceptorContext + SupervisorJob(scope.coroutineContext.job),
            sendRequestToStore = dispatchChannel::send
        )
        val notificationFlow: Flow<FluxoEvent<Intent, State, SideEffect>> = notifications.transformWhile {
            emit(it)
            it !is FluxoEvent.StoreCleared
        }
        for (interceptor in conf.interceptors) {
            with(interceptor) {
                try {
                    interceptorScope.start(notificationFlow)
                } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                    notifications.tryEmit(FluxoEvent.UnhandledError(this@FluxoStore, e))
                }
            }
        }
        notifications.tryEmit(FluxoEvent.StoreStarted(this))
    }

    private fun filterRequest(request: StoreRequest<Intent, State>): Boolean {
        when (request) {
            is StoreRequest.RestoreState -> {
                // when restoring state, always accept the item
                return true
            }

            is StoreRequest.HandleIntent -> {
                // when handling an Intent, check with the IntentFilter to see if it should be accepted
                val currentState = mutableState.value
                val shouldAcceptIntent = conf.intentFilter?.invoke(currentState, request.intent) ?: true

                if (!shouldAcceptIntent) {
                    notifications.tryEmit(FluxoEvent.IntentRejected(this, currentState, request.intent))
                    request.deferred?.complete(Unit)
                }

                return shouldAcceptIntent
            }
        }
    }

    @Suppress("NestedBlockDepth")
    private suspend fun safelyHandleIntent(intent: Intent, deferred: CompletableDeferred<Unit>?, guardian: InputStrategy.Guardian?) {
        notifications.emit(FluxoEvent.IntentAccepted(this, intent))

        val stateBeforeCancellation = mutableState.value
        try {
            // Create a handler scope to handle the intent normally
            val handlerScope = IntentHandlerScopeImpl(
                guardian = guardian,
                getState = mutableState::value,
                updateStateAndGet = ::updateStateAndGet,
                sendSideEffect = ::postSideEffect,
                sendSideJob = {
                    sideJobsChannel.trySend(it)
                    notifications.tryEmit(FluxoEvent.SideJobQueued(this@FluxoStore, it.key))
                },
            )
            try {
                with(handlerScope) {
                    with(intentHandler) {
                        handleIntent(intent)
                    }
                }
                notifications.emit(FluxoEvent.IntentHandledSuccessfully(this@FluxoStore, intent))
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                notifications.emit(FluxoEvent.IntentHandlerError(this@FluxoStore, intent, e))
            }
            handlerScope.close()
            deferred?.complete(Unit)
        } catch (_: CancellationException) {
            // when the coroutine is cancelled for any reason, we must assume the intent did not
            // complete and may have left the State in a bad, erm..., state. We should reset it and
            // try to forget that we ever tried to process it in the first place
            notifications.emit(FluxoEvent.IntentCancelled(this, intent))
            if (conf.inputStrategy.rollbackOnCancellation) {
                updateState(stateBeforeCancellation)
            }
            deferred?.complete(Unit)
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            notifications.emit(FluxoEvent.IntentHandlerError(this, intent, e))
            deferred?.complete(Unit)
        }
    }

    private suspend fun safelyStartSideJob(key: String, block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit) {
        val map = sideJobsMap
        val prevJob = map[key]
        val restartState = if (prevJob != null) RestartState.Restarted else RestartState.Initial

        // Cancel if we have a side-job already running
        prevJob?.run {
            job?.cancel()
            job = null
        }

        // Go through and remove any side-jobs that have completed (either by
        // cancellation or because they finished on their own)
        map.entries
            .filterNot { it.value.job?.isActive == true }
            .map { it.key }
            .forEach { map.remove(it) }

        // Launch a new side-job in its own isolated coroutine scope where:
        //   1) it is cancelled when the storeScope is cancelled
        //   2) errors are caught by the uncaughtExceptionHandler for crash reporting
        //   3) has a supervisor job, so we can cancel the side-job without cancelling the whole storeScope
        //
        // Consumers of this side-job can launch many jobs, and all will be cancelled together when the
        // side-job is restarted or the storeScope is cancelled.
        map[key] = RunningSideJob(
            key = key,
            block = block,
            job = sideJobScope.launch {
                notifications.emit(FluxoEvent.SideJobStarted(this@FluxoStore, key, restartState))
                try {
                    SideJobScopeImpl<Intent, State, SideEffect>(
                        sendIntentToStore = { dispatchChannel.send(StoreRequest.HandleIntent(null, it)) },
                        sendSideEffectToStore = ::postSideEffect,
                        currentStateWhenStarted = mutableState.value,
                        restartState = restartState,
                        coroutineScope = this,
                    ).block()
                    notifications.emit(FluxoEvent.SideJobCompleted(this@FluxoStore, key, restartState))
                } catch (_: CancellationException) {
                    notifications.emit(FluxoEvent.SideJobCancelled(this@FluxoStore, key, restartState))
                } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                    notifications.emit(FluxoEvent.SideJobError(this@FluxoStore, key, restartState, e))
                }
            }
        )
    }

    /**
     * Called on [scope] completion for any reason.
     */
    private fun onShutdown(cause: Throwable?) {
        val ce = cause.toCancellationException()
        for (value in sideJobsMap.values) {
            value.job?.cancel(ce)
            value.job = null
        }
        sideJobsMap.clear()

        notifications.tryEmit(FluxoEvent.StoreCleared(this, cause))

        dispatchChannel.close(ce)
        sideEffectChannel.close(ce)
        sideJobsChannel.close(ce)
    }

    // endregion
}