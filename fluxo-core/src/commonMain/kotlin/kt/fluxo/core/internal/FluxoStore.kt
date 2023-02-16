@file:Suppress("TooManyFunctions", "TooGenericExceptionCaught")

package kt.fluxo.core.internal

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kt.fluxo.core.Bootstrapper
import kt.fluxo.core.FluxoClosedException
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.IntentHandler
import kt.fluxo.core.SideEffectsStrategy
import kt.fluxo.core.SideJob
import kt.fluxo.core.StoreSE
import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.data.GuaranteedEffect
import kt.fluxo.core.debug.DEBUG
import kt.fluxo.core.debug.debugIntentWrapper
import kt.fluxo.core.dsl.InputStrategyScope
import kt.fluxo.core.intercept.StoreRequest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmSynthetic

@PublishedApi
@InternalFluxoApi
internal class FluxoStore<Intent, State, SideEffect : Any>(
    initialState: State,
    private val intentHandler: IntentHandler<Intent, State, SideEffect>,
    conf: FluxoSettings<Intent, State, SideEffect>,
) : StoreSE<Intent, State, SideEffect> {

    // region Fields, initialization

    private val mutableState = MutableStateFlow(initialState)

    // TODO: Generate name from Store host with reflection in debug mode?
    override val name: String = conf.name ?: "store#${storeNumber.getAndIncrement()}"

    private val sideJobScope: CoroutineScope
    private val sideJobsMap = ConcurrentHashMap<String, RunningSideJob>()
    private val sideJobsMutex = Mutex()

    private val sideEffectChannel: Channel<SideEffect>?
    private val sideEffectFlowField: Flow<SideEffect>?
    override val sideEffectFlow: Flow<SideEffect>
        get() = sideEffectFlowField ?: error("Side effects are disabled for the current store: $name")

    private val debugChecks = conf.debugChecks
    private val closeOnExceptions = conf.closeOnExceptions
    private val bootstrapper = conf.bootstrapper
    private val intentFilter = conf.intentFilter
    private val coroutineStart = if (!conf.optimized) CoroutineStart.DEFAULT else CoroutineStart.UNDISPATCHED

    private val inputStrategy = conf.inputStrategy
    override val coroutineContext: CoroutineContext

    @InternalFluxoApi
    internal val intentContext: CoroutineContext

    override val subscriptionCount: StateFlow<Int>

    private val requestsChannel: Channel<StoreRequest<Intent, State>>

    private val initialised = atomic(false)

    init {
        val ctx = conf.coroutineContext + (conf.scope?.coroutineContext ?: EmptyCoroutineContext) + when {
            !debugChecks -> EmptyCoroutineContext
            else -> CoroutineName(toString())
        }

        val parent = ctx[Job] ?: conf.intentContext[Job]
        val job = if (closeOnExceptions) Job(parent) else SupervisorJob(parent)
        job.invokeOnCompletion(::onShutdown)

        val parentExceptionHandler = conf.exceptionHandler ?: ctx[CoroutineExceptionHandler]
        val exceptionHandler = CoroutineExceptionHandler { context, e ->
            parentExceptionHandler?.handleException(context, e)
            if (closeOnExceptions) {
                try {
                    val ce = CancellationException("Uncaught exception: $e", e)
                    @Suppress("UNINITIALIZED_VARIABLE")
                    this@FluxoStore.coroutineContext.cancel(ce)
                    context.cancel(ce)
                } catch (e2: Throwable) {
                    e2.addSuppressed(e)
                    throw e2
                }
            }
        }
        coroutineContext = ctx + exceptionHandler + job
        val scope: CoroutineScope = this

        intentContext = scope.coroutineContext + conf.intentContext + when {
            !debugChecks -> EmptyCoroutineContext
            else -> CoroutineName("$F[$name:intentScope]")
        }

        sideJobScope = if (intentHandler is ReducerIntentHandler && bootstrapper == null) {
            // Minor optimization as side jobs are off when bootstrapper is null and ReducerIntentHandler set.
            scope
        } else {
            scope + conf.sideJobsContext + exceptionHandler + SupervisorJob(job) + when {
                !debugChecks -> EmptyCoroutineContext
                else -> CoroutineName("$F[$name:sideJobScope]")
            }
        }

        // Leak-free transfer via channel
        // https://github.com/Kotlin/kotlinx.coroutines/issues/1936
        // See "Undelivered elements" section in Channel documentation for details.
        //
        // Handled cases:
        // — sending operation cancelled before it had a chance to actually send the element.
        // — receiving operation retrieved the element from the channel cancelled when trying to return it the caller.
        // — channel cancelled, in which case onUndeliveredElement called on every remaining element in the channel's buffer.
        val intentResendLock = if (inputStrategy.resendUndelivered) Mutex() else null
        requestsChannel = inputStrategy.createQueue {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                when (it) {
                    is StoreRequest.HandleIntent -> {
                        // Only one resending per moment to protect from the recursion.
                        var resent = false
                        if (isActive && intentResendLock != null && intentResendLock.tryLock()) {
                            try {
                                @Suppress("UNINITIALIZED_VARIABLE")
                                resent = requestsChannel.trySend(it).isSuccess
                            } catch (_: Throwable) {
                            } finally {
                                intentResendLock.unlock()
                            }
                        }
                        if (!resent) {
                            it.intent.closeSafely()
                            it.deferred.cancel()
                        }
                    }

                    is StoreRequest.RestoreState -> updateState(it)
                }
            }
        }

        // Prepare side effects handling, considering all possible strategies
        var subscriptionCount = mutableState.subscriptionCount
        val sideEffectsSubscriptionCount: StateFlow<Int>?
        val sideEffectsStrategy = conf.sideEffectsStrategy
        if (sideEffectsStrategy === SideEffectsStrategy.DISABLE) {
            // Disable side effects
            sideEffectChannel = null
            sideEffectFlowField = null
            sideEffectsSubscriptionCount = null
        } else if (sideEffectsStrategy is SideEffectsStrategy.SHARE) {
            // MutableSharedFlow-based SideEffect
            sideEffectChannel = null
            val flow = MutableSharedFlow<SideEffect>(
                replay = sideEffectsStrategy.replay,
                extraBufferCapacity = conf.sideEffectBufferSize,
                onBufferOverflow = BufferOverflow.SUSPEND,
            )
            sideEffectsSubscriptionCount = flow.subscriptionCount
            sideEffectFlowField = flow
        } else {
            val seResendLock = Mutex()
            val channel = Channel<SideEffect>(conf.sideEffectBufferSize, BufferOverflow.SUSPEND) {
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    // Only one resending per moment to protect from the recursion.
                    var resent = false
                    if (isActive && seResendLock.tryLock()) {
                        try {
                            @Suppress("UNINITIALIZED_VARIABLE")
                            resent = sideEffectChannel!!.trySend(it).isSuccess
                        } catch (_: Throwable) {
                        } finally {
                            seResendLock.unlock()
                        }
                    }
                    if (!resent) {
                        it.closeSafely()
                    }
                }
            }
            sideEffectChannel = channel
            sideEffectsSubscriptionCount = MutableStateFlow(0)
            val flow = if (sideEffectsStrategy === SideEffectsStrategy.CONSUME) {
                channel.consumeAsFlow()
            } else {
                check(!debugChecks || sideEffectsStrategy === SideEffectsStrategy.RECEIVE)
                channel.receiveAsFlow()
            }
            sideEffectFlowField = SubscriptionCountFlow(sideEffectsSubscriptionCount, flow)
        }
        if (sideEffectsSubscriptionCount != null) {
            subscriptionCount += sideEffectsSubscriptionCount
        }
        this.subscriptionCount = subscriptionCount

        // Start the Store
        if (!conf.lazy) {
            // Eager start
            start()
        } else {
            // Lazy start on state subscription
            val subscriptions = subscriptionCount
            scope.launch(
                when {
                    !debugChecks -> EmptyCoroutineContext
                    else -> CoroutineName("$F[$name:lazyStart]")
                },
                start = coroutineStart,
            ) {
                // start on the first subscriber.
                subscriptions.first { it > 0 }
                if (isActive && this@FluxoStore.isActive) {
                    start()
                }
            }
        }
    }

    override fun start(): Job? {
        if (!isActive) {
            throw FluxoClosedException(
                "Store is closed, it cannot be restarted",
                cancellationCause.let { it.cause ?: it },
            )
        }
        if (initialised.compareAndSet(expect = false, update = true)) {
            return launch()
        }
        return null
    }

    // endregion


    // region Intents machinery

    override suspend fun emit(value: Intent) {
        start()
        val i = if (DEBUG && debugChecks) debugIntentWrapper(value) else value
        val request = StoreRequest.HandleIntent<Intent, State>(i)
        requestsChannel.send(request)
    }

    override fun send(intent: Intent): Job {
        start()
        val i = if (DEBUG && debugChecks) debugIntentWrapper(intent) else intent
        val request = StoreRequest.HandleIntent<Intent, State>(i)
        launch(start = CoroutineStart.UNDISPATCHED) {
            requestsChannel.send(request)
        }
        return request.deferred
    }

    // endregion


    // region State control

    override var value: State
        get() = mutableState.value
        set(value) {
            updateStateAndGet { value }
        }

    private fun updateState(request: StoreRequest.RestoreState<Intent, State>): State {
        try {
            val state = updateState(request.state)
            request.deferred.complete(Unit)
            return state
        } catch (e: Throwable) {
            request.deferred.cancel(e.toCancellationException())
            throw e
        }
    }

    private fun updateState(nextValue: State): State = updateStateAndGet { nextValue }

    private fun updateStateAndGet(function: (State) -> State): State {
        while (true) {
            val prevValue = mutableState.value
            val nextValue = function(prevValue)
            if (mutableState.compareAndSet(prevValue, nextValue)) {
                if (prevValue != nextValue) {
                    prevValue.closeSafely()
                }
                return nextValue
            }
            // TODO: we may need to close nextValue here
            coroutineContext.ensureActive()
        }
    }

    // endregion


    // region Internals

    private suspend fun postSideEffect(sideEffect: SideEffect) {
        try {
            if (!isActive) {
                throw cancellationCause
            }
            if (sideEffect is GuaranteedEffect<*>) {
                sideEffect.setResendFunction(::postSideEffectSync)
            }
            sideEffectChannel?.send(sideEffect)
                ?: (sideEffectFlowField as MutableSharedFlow).emit(sideEffect)
        } catch (e: CancellationException) {
            sideEffect.closeSafely(e.cause)
        } catch (e: Throwable) {
            sideEffect.closeSafely(e)
            handleException(currentCoroutineContext(), e)
        }
    }

    private fun postSideEffectSync(sideEffect: SideEffect) {
        launch(start = CoroutineStart.UNDISPATCHED) {
            postSideEffect(sideEffect)
        }
    }

    /** Called only once for each [FluxoStore] */
    private fun launch() = launch(
        when {
            !debugChecks -> EmptyCoroutineContext
            else -> CoroutineName("$F[$name:launch]")
        },
        start = coroutineStart,
    ) {
        // observe and process intents
        val requestsFlow = requestsChannel.receiveAsFlow()
        val inputStrategy = inputStrategy
        val inputStrategyScope: InputStrategyScope<StoreRequest<Intent, State>> = { request ->
            when (request) {
                is StoreRequest.HandleIntent -> {
                    if (debugChecks) {
                        withContext(CoroutineName("$F[$name <= Intent ${request.intent}]")) {
                            safelyHandleIntent(request.intent, request.deferred)
                        }
                    } else {
                        safelyHandleIntent(request.intent, request.deferred)
                    }
                }

                is StoreRequest.RestoreState -> updateState(request)
            }
        }
        launch(intentContext, start = coroutineStart) {
            with(inputStrategy) {
                inputStrategyScope.processRequests(requestsFlow)
            }
        }

        // bootstrap
        bootstrapper?.let { bootstrapper ->
            safelyRunBootstrapper(bootstrapper)
        }
    }

    private suspend fun safelyHandleIntent(intent: Intent, deferred: CompletableDeferred<Unit>?) {
        val stateBeforeCancellation = mutableState.value
        try {
            // Apply an intent filter before processing, if defined
            intentFilter?.also { accept ->
                if (!accept(stateBeforeCancellation, intent)) {
                    return
                }
            }

            with(intentHandler) {
                // Await all children completions with coroutineScope
                coroutineScope {
                    val storeScope = StoreScopeImpl(
                        job = intent,
                        guardian = when {
                            !debugChecks -> null
                            else -> InputStrategyGuardian(
                                parallelProcessing = inputStrategy.parallelProcessing,
                                isBootstrap = false,
                                intent = intent,
                                handler = intentHandler,
                            )
                        },
                        getState = mutableState::value,
                        updateStateAndGet = ::updateStateAndGet,
                        sendSideEffect = ::postSideEffect,
                        sendSideJob = ::postSideJob,
                        subscriptionCount = subscriptionCount,
                        coroutineContext = coroutineContext,
                    )
                    storeScope.handleIntent(intent)
                    storeScope.close()
                }
            }
            deferred?.complete(Unit)
        } catch (ce: CancellationException) {
            deferred?.completeExceptionally(ce)
            // reset state as intent did not complete successfully
            if (inputStrategy.rollbackOnCancellation) {
                updateState(stateBeforeCancellation)
            }
        } catch (e: Throwable) {
            deferred?.completeExceptionally(e)
            handleException(currentCoroutineContext(), e)
        }
    }


    // region Side jobs

    private suspend fun postSideJob(
        key: String,
        context: CoroutineContext,
        start: CoroutineStart,
        block: SideJob<Intent, State, SideEffect>,
    ): Job {
        check(this !== sideJobScope) { "Side jobs are disabled for the current store: $name" }

        val map = sideJobsMap
        sideJobsMutex.withLock(if (debugChecks) key else null) {
            val prevSj = map[key]

            // Cancel already running sideJob
            prevSj?.run {
                job?.cancel()
                job = null
            }

            // Go through and remove any completed sideJobs (either cancelled or finished)
            val iterator = map.values.iterator()
            for (sj in iterator) {
                if (sj.job?.isActive != true) {
                    iterator.remove()
                }
            }

            // Launch a new sideJob in its own isolated coroutine scope where:
            //   1) cancelled when the scope or sideJobScope cancelled
            //   2) errors caught for crash reporting
            //   3) has a supervisor job, to cancel the sideJob without cancelling whole store scope
            //
            // Consumers of this sideJob can launch many jobs, and all cancelled together when the
            // sideJob restarted, or the store scope cancelled.
            val wasRestarted = prevSj != null
            val coroutineContext = when {
                !debugChecks -> context
                else -> {
                    val parent = currentCoroutineContext()[CoroutineName]?.name
                    val name = if (parent != null) "$parent => SideJob $key" else "$F[$name SideJob $key]"
                    CoroutineName(name) + context
                }
            }
            // TODO: Test if job awaits for all potential children completions when join
            val job = sideJobScope.launch(context = coroutineContext, start = start) {
                try {
                    val sideJobScope = SideJobScopeImpl(
                        updateStateAndGet = ::updateStateAndGet,
                        emitIntent = ::emit,
                        sendIntent = ::send,
                        sendSideEffect = ::postSideEffect,
                        currentStateWhenStarted = mutableState.value,
                        coroutineScope = this,
                    )

                    // Await all children completions with coroutineScope
                    coroutineScope {
                        sideJobScope.block(wasRestarted)
                    }
                } catch (e: CancellationException) {
                    // rethrow, cancel the job and all children
                    throw e
                } catch (e: Throwable) {
                    handleException(currentCoroutineContext(), e)
                }
            }
            map[key] = RunningSideJob(job = job)

            return job
        }
    }

    // endregion

    private suspend fun safelyRunBootstrapper(bootstrapper: Bootstrapper<Intent, State, SideEffect>) = withContext(intentContext) {
        try {
            // Await all children completions with coroutineScope
            coroutineScope {
                val bootstrapperScope = BootstrapperScopeImpl(
                    bootstrapper = bootstrapper,
                    guardian = if (!debugChecks) {
                        null
                    } else {
                        InputStrategyGuardian(
                            parallelProcessing = inputStrategy.parallelProcessing,
                            isBootstrap = true,
                            intent = null,
                            handler = bootstrapper,
                        )
                    },
                    getState = mutableState::value,
                    updateStateAndGet = ::updateStateAndGet,
                    emitIntent = ::emit,
                    sendIntent = ::send,
                    sendSideEffect = ::postSideEffect,
                    sendSideJob = ::postSideJob,
                    subscriptionCount = subscriptionCount,
                    coroutineContext = coroutineContext,
                )
                bootstrapperScope.bootstrapper()
                bootstrapperScope.close()
            }
        } catch (_: CancellationException) {
        } catch (e: Throwable) {
            handleException(currentCoroutineContext(), e)
        }
    }

    // endregion


    // region Other

    private fun handleException(context: CoroutineContext, exception: Throwable) {
        if (!closeOnExceptions) {
            val handler = coroutineContext[CoroutineExceptionHandler]
            if (handler != null) {
                handler.handleException(context, exception)
                return
            }
        }
        throw exception
    }

    private fun Any?.closeSafely(cause: Throwable? = null, ctx: CoroutineContext? = null) {
        if (this is Closeable) {
            try {
                close() // Close if Closeable resource
            } catch (e: Throwable) {
                if (cause != null) {
                    e.addSuppressed(cause)
                }
                handleException(ctx ?: coroutineContext, e)
            }
        }
    }

    /**
     * @throws IllegalStateException when invoked while [coroutineContext] still active.
     */
    @OptIn(InternalCoroutinesApi::class)
    private val cancellationCause get() = coroutineContext[Job]!!.getCancellationException()

    /**
     * Called on the shutdown for any reason.
     */
    private fun onShutdown(cause: Throwable?) {
        val cancellationCause = cause.toCancellationException() ?: cancellationCause
        val ceCause = cancellationCause.cause

        // Useful if intentContext has an ovirriden Job
        intentContext.cancel(cancellationCause)

        // Cancel and clear sideJobs
        for (value in sideJobsMap.values) {
            value.job?.cancel(cancellationCause)
            value.job = null
        }
        sideJobsMap.clear()

        // Close and clear state & side effects machinery.
        // FIXME: Test case when interceptorScope is already closed here
        @OptIn(ExperimentalCoroutinesApi::class)
        launch(Dispatchers.Unconfined + Job(), start = CoroutineStart.UNDISPATCHED) {
            mutableState.value.closeSafely(ceCause)
            (sideEffectFlowField as? MutableSharedFlow)?.apply {
                val replayCache = replayCache
                resetReplayCache()
                replayCache.forEach { it.closeSafely(ceCause) }
            }
            sideEffectChannel?.apply {
                while (!isEmpty) {
                    val result = receiveCatching()
                    if (result.isClosed) break
                    result.getOrNull()?.closeSafely(ceCause)
                }
                close(ceCause)
            }
        }
    }

    override fun toString(): String = "$F[$name]"

    private companion object {
        private const val F = "Fluxo"
        private val storeNumber = atomic(0)
    }

    // endregion


    // region StateFlow methods

    @get:JvmSynthetic
    override val replayCache: List<State> get() = mutableState.replayCache

    @JvmSynthetic
    override suspend fun collect(collector: FlowCollector<State>) = mutableState.collect(collector)

    // endregion
}
