@file:Suppress("TooManyFunctions", "TooGenericExceptionCaught")

package kt.fluxo.core.internal

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
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
import kt.fluxo.common.annotation.InternalFluxoApi
import kt.fluxo.core.Bootstrapper
import kt.fluxo.core.FluxoClosedException
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.IntentHandler
import kt.fluxo.core.SideEffectStrategy
import kt.fluxo.core.SideJob
import kt.fluxo.core.data.GuaranteedEffect
import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.factory.StoreDecorator
import kt.fluxo.core.intent.IntentStrategy
import kt.fluxo.core.intent.IntentStrategyScope
import kt.fluxo.core.updateState
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@InternalFluxoApi
internal class FluxoStore<Intent, State, SideEffect : Any>(
    initialState: State,
    private val intentHandler: IntentHandler<Intent, State, SideEffect>,
    conf: FluxoSettings<Intent, State, SideEffect>,
) : AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler,
    StoreDecorator<Intent, State, SideEffect>,
    IntentStrategyScope<Intent, State> {

    // region Fields, initialization

    // TODO: Generate a name from Store host with reflection in debug mode?
    override val name: String = conf.name ?: "store#${storeNumber.getAndIncrement()}"

    private val mutableState = MutableStateFlow(initialState)
    override val subscriptionCount: StateFlow<Int>
    override val coroutineContext: CoroutineContext

    private val intentStrategy: IntentStrategy<Intent, State>
    private val intentFilter = conf.intentFilter

    private val sideEffectChannel: Channel<SideEffect>?
    private val sideEffectFlowField: Flow<SideEffect>?
    override val sideEffectFlow: Flow<SideEffect>
        get() = sideEffectFlowField ?: error("Side effects are disabled for the current store: $name")

    private val sideJobScope: CoroutineScope
    private val sideJobsMap: MutableMap<String, RunningSideJob>?
    private val sideJobsMutex: Mutex?

    private val debugChecks = conf.debugChecks
    private val closeOnExceptions = conf.closeOnExceptions
    private val exceptionHandler: CoroutineExceptionHandler?

    private val hasReducerIntentHandler = intentHandler is ReducerHandler
    private lateinit var decorator: StoreDecorator<Intent, State, SideEffect>
    private lateinit var startJob: Job

    init {
        // region Prepare coroutine context
        val debugChecks = conf.debugChecks
        val ctx = conf.coroutineContext + (conf.scope?.coroutineContext ?: EmptyCoroutineContext) + when {
            !debugChecks -> EmptyCoroutineContext
            else -> CoroutineName("$F[$name]")
        }

        val job = ctx[Job].let { if (closeOnExceptions) Job(it) else SupervisorJob(it) }
        job.invokeOnCompletion(::onShutdown)

        exceptionHandler = conf.exceptionHandler ?: ctx[CoroutineExceptionHandler]
        val exceptionHandler: CoroutineExceptionHandler = this
        coroutineContext = ctx + exceptionHandler + job
        val scope: CoroutineScope = this
        // endregion

        // Side jobs handling
        // Minor optimization as side jobs are off when bootstrapper is null and ReducerIntentHandler set.
        sideJobScope = if (!hasReducerIntentHandler || conf.bootstrapper != null) {
            sideJobsMap = ConcurrentHashMap()
            sideJobsMutex = Mutex()
            scope + conf.sideJobsContext + exceptionHandler + SupervisorJob(job) + when {
                !debugChecks -> EmptyCoroutineContext
                else -> CoroutineName("$F[$name:sideJobScope]")
            }
        } else {
            sideJobsMap = null
            sideJobsMutex = null
            scope
        }

        // Input handling
        intentStrategy = conf.intentStrategy(scope = this)

        // region Prepare side effects handling, considering all possible strategies
        var subscriptionCount = mutableState.subscriptionCount
        val sideEffectsSubscriptionCount: StateFlow<Int>?
        val sideEffectsStrategy = conf.sideEffectStrategy
        if (hasReducerIntentHandler && debugChecks) {
            require(sideEffectsStrategy == SideEffectStrategy.DISABLE) {
                "Expected SideEffectsStrategy.DISABLE to be set as a sideEffectsStrategy. " +
                    "Please, fill an issue if you think it should be done automatically here."
            }
        }
        if (sideEffectsStrategy === SideEffectStrategy.DISABLE) {
            // Disable side effects
            sideEffectChannel = null
            sideEffectFlowField = null
            sideEffectsSubscriptionCount = null
        } else if (sideEffectsStrategy is SideEffectStrategy.SHARE) {
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
            // Leak-free transfer via channel
            // https://github.com/Kotlin/kotlinx.coroutines/issues/1936
            // See "Undelivered elements" section in Channel documentation for details.
            //
            // Handled cases:
            // — sending operation cancelled before it had a chance to actually send the element.
            // — receiving operation retrieved the element from the channel cancelled
            //   when trying to return it the caller.
            // — channel cancelled, in which case onUndeliveredElement called
            //   on every remaining element in the channel's buffer.
            val seResendLock = Mutex()
            val channel = Channel<SideEffect>(conf.sideEffectBufferSize, BufferOverflow.SUSPEND) {
                var resent = false
                // Only one resending per moment to protect from the recursion.
                if (coroutineContext.isActive && seResendLock.tryLock()) {
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
                decorator.onUndeliveredSideEffect(it, resent)
            }
            sideEffectChannel = channel
            sideEffectsSubscriptionCount = MutableStateFlow(0)
            val flow = if (sideEffectsStrategy === SideEffectStrategy.CONSUME) {
                channel.consumeAsFlow()
            } else {
                check(!debugChecks || sideEffectsStrategy === SideEffectStrategy.RECEIVE)
                channel.receiveAsFlow()
            }
            sideEffectFlowField = SubscriptionCountFlow(sideEffectsSubscriptionCount, flow)
        }
        if (sideEffectsSubscriptionCount != null) {
            subscriptionCount += sideEffectsSubscriptionCount
        }
        // endregion
        this.subscriptionCount = subscriptionCount
    }

    override fun init(decorator: StoreDecorator<Intent, State, SideEffect>, conf: FluxoSettings<Intent, State, SideEffect>) {
        this.decorator = decorator

        // Start the Store
        val scope: CoroutineScope = this
        val coroutineStart = if (!conf.optimized) CoroutineStart.DEFAULT else CoroutineStart.UNDISPATCHED
        val bootstrapper = conf.bootstrapper
        val startJob = scope.launch(
            context = if (!debugChecks) EmptyCoroutineContext else CoroutineName("$F[$name:launch]"),
            start = if (conf.lazy) CoroutineStart.LAZY else coroutineStart,
        ) { decorator.onStart(bootstrapper) }
        this.startJob = startJob

        // Lazy start on state subscription
        if (conf.lazy) {
            // Support overriding + catch only variable in the launched lambda instead of the store.
            val subscriptions = this.subscriptionCount
            scope.launch(
                context = if (!debugChecks) EmptyCoroutineContext else CoroutineName("$F[$name:lazyStart]"),
                start = coroutineStart,
            ) {
                // start on the first subscriber.
                subscriptions.first { it > 0 }
                startJob.start()
            }
        }
    }

    override fun start(): Job {
        if (!coroutineContext.isActive) {
            throw FluxoClosedException(
                "Store is closed, it cannot be restarted",
                cancellationCause.let { it.cause ?: it },
            )
        }
        return startJob.apply { start() }
    }

    override suspend fun onStart(bootstrapper: Bootstrapper<Intent, State, SideEffect>?) {
        // Observe and process intents, launching strategy if needed.
        // • Use store scope to not block the startJob completion.
        // • Re-save intent strategy to avoid the store capture in the launched lambda.
        val intentStrategy = intentStrategy
        if (intentStrategy.isLaunchNeeded) {
            val storeJob = checkNotNull(coroutineContext[Job])
            (this as CoroutineScope).launch(
                context = if (!debugChecks) EmptyCoroutineContext else CoroutineName("$F[$name:intentStrategy]"),
                start = CoroutineStart.UNDISPATCHED,
            ) {
                try {
                    // Can suspend until the store closed!
                    intentStrategy.launch()
                } catch (ce: CancellationException) {
                    if (storeJob.isActive) {
                        throw IllegalStateException("Intent strategy shouldn't be cancelled while store is active", ce.cause ?: ce)
                    }
                }
            }
        }

        bootstrapper?.let { bs ->
            try {
                decorator.onBootstrap(bs)
            } catch (ce: CancellationException) {
                if (closeOnExceptions) {
                    handleException(currentCoroutineContext(), ce)
                }
            } catch (e: Throwable) {
                handleException(currentCoroutineContext(), e)
            }
        }
    }


    override suspend fun onBootstrap(bootstrapper: Bootstrapper<Intent, State, SideEffect>) {
        val guardedScope: StoreScope<Intent, State, SideEffect>? = when {
            !debugChecks -> null
            else -> GuardedStoreDecorator(
                guardian = IntentStrategyGuardian(
                    parallelProcessing = intentStrategy.parallelProcessing,
                    isBootstrap = true,
                    intent = null,
                    handler = bootstrapper,
                ),
                store = decorator,
            )
        }
        try {
            (guardedScope ?: decorator).bootstrapper()
        } finally {
            guardedScope?.close()
        }
    }

    // endregion


    // region Intents machinery

    override suspend fun emit(value: Intent) {
        start()
        intentStrategy.queueIntentSuspend(value)
    }

    override fun send(intent: Intent): Job {
        start()
        return intentStrategy.queueIntent(intent)
    }

    override suspend fun executeIntent(intent: Intent) {
        val decorator = decorator
        if (!debugChecks) {
            decorator.onIntent(intent)
        } else {
            withContext(CoroutineName("$F[$name <= Intent $intent]")) {
                decorator.onIntent(intent)
            }
        }
    }

    override suspend fun onIntent(intent: Intent) {
        // TODO: Move the intent filter before queing or completely replace with decorators?

        // Apply an intent filter before processing, if defined
        intentFilter?.also { accept ->
            if (!accept(value, intent)) {
                return
            }
        }

        val guardedScope: StoreScope<Intent, State, SideEffect>? = when {
            !debugChecks -> null
            else -> GuardedStoreDecorator(
                guardian = IntentStrategyGuardian(
                    parallelProcessing = intentStrategy.parallelProcessing,
                    isBootstrap = false,
                    intent = intent,
                    handler = intentHandler,
                ),
                store = decorator,
            )
        }
        val scope = guardedScope ?: decorator
        with(intentHandler) {
            // ReducerIntentHandler isn't suspendable
            if (hasReducerIntentHandler) {
                scope.handleIntent(intent)
            } else {
                // Await all children completions with coroutineScope
                // TODO: Can we awoid lambda creation?
                coroutineScope {
                    scope.handleIntent(intent)
                }
            }
        }
        guardedScope?.close()
    }

    override fun undeliveredIntent(intent: Intent, wasResent: Boolean) {
        if (!wasResent) {
            intent.closeSafely()
        }
        decorator.onUndeliveredIntent(intent, wasResent)
    }

    // endregion


    // region State control

    override var value: State
        get() = mutableState.value
        set(value) {
            updateState { value }
        }

    override fun compareAndSet(expect: State, update: State): Boolean {
        if (mutableState.compareAndSet(expect, update)) {
            /**
             * If both [expect] and [update] are equal to the current [value], [compareAndSet] returns true,
             * but doesn't actually change the stored reference!
             */
            if (expect != update) {
                expect.closeSafely()
            } else if (expect !== update) {
                update.closeSafely()
            }
            decorator.onStateChanged(update, expect)
            return true
        }
        if (!coroutineContext.isActive) {
            update.closeSafely()
            throw cancellationCause
        }
        return false
    }

    // endregion


    // region Side effects

    /** Cache of the [postSideEffect] reference for the [GuaranteedEffect] support */
    private val resendFun = atomic<((sideEffect: SideEffect) -> Unit)?>(null)

    override suspend fun postSideEffect(sideEffect: SideEffect) {
        try {
            if (!coroutineContext.isActive) {
                throw cancellationCause
            }
            if (sideEffect is GuaranteedEffect<*>) {
                // Use cached reference when possible
                var rf = resendFun.value
                if (rf == null) {
                    rf = { se ->
                        // Don't pay for dispatch here, it is never necessary
                        (this as CoroutineScope).launch(start = CoroutineStart.UNDISPATCHED) {
                            postSideEffect(se)
                        }
                    }
                    resendFun.compareAndSet(null, rf)
                }
                sideEffect.setResendFunction(rf)
            }
            sideEffectChannel?.send(sideEffect)
                ?: (sideEffectFlowField as MutableSharedFlow).emit(sideEffect)
        } catch (ce: CancellationException) {
            sideEffect.closeSafely(ce.cause ?: ce)
            decorator.onUndeliveredSideEffect(sideEffect, wasResent = false)
        } catch (e: Throwable) {
            sideEffect.closeSafely(e)
            decorator.onUndeliveredSideEffect(sideEffect, wasResent = false)
            handleException(currentCoroutineContext(), e)
        }
    }

    // endregion


    // region Side jobs

    override suspend fun sideJob(
        key: String,
        context: CoroutineContext,
        start: CoroutineStart,
        onError: ((error: Throwable) -> Unit)?,
        block: SideJob<Intent, State, SideEffect>,
    ): Job {
        val map = checkNotNull(sideJobsMap) { "Side jobs are disabled for the current store: $name" }
        // Avoid concurrent side jobs queuing with mutex
        checkNotNull(sideJobsMutex).withLock(if (debugChecks) key else null) {
            val prevSj = map[key]

            // Cancel if we have a sideJob already running
            prevSj?.run {
                job?.cancel()
                job = null
            }

            // Go through and remove any sideJobs that have completed (either by
            // cancellation or because they finished on their own)
            val iterator = map.values.iterator()
            for (sj in iterator) {
                if (sj.job?.isActive != true) {
                    iterator.remove()
                }
            }

            // Launch a new sideJob in its own isolated coroutine scope where:
            //   1) cancelled when the scope or sideJobScope cancelled.
            //   2) errors caught for crash reporting.
            //   3) has a supervisor job, to cancel the sideJob without cancelling whole store scope.
            //
            // Consumers of this sideJob can launch many jobs, and all will be cancelled together when the
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
                    decorator.onSideJob(key, wasRestarted, block)
                } catch (ce: CancellationException) {
                    // rethrow, cancel the job and all children
                    throw ce
                } catch (e: Throwable) {
                    if (onError != null) {
                        try {
                            onError(e)
                        } catch (e2: Throwable) {
                            e2.addSuppressed(e)
                            handleException(currentCoroutineContext(), e2)
                        }
                    } else {
                        handleException(currentCoroutineContext(), e)
                    }
                }
            }
            map[key] = RunningSideJob(job = job)

            return job
        }
    }

    override suspend fun onSideJob(key: String, wasRestarted: Boolean, sideJob: SideJob<Intent, State, SideEffect>) {
        this.sideJob(wasRestarted)
    }

    // endregion


    // region Other

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        var rethrow = false
        try {
            val ce = exception.toCancellationException()
            val error = ce?.cause ?: exception
            if (!decorator.onUnhandledError(error) && closeOnExceptions && context[Job]?.isActive != false) {
                coroutineContext.cancel(ce)
                context.cancel(ce)
                rethrow = true
            }
            exceptionHandler?.handleException(context, error)
        } catch (e: Throwable) {
            e.addSuppressed(exception)
            throw e
        }
        if (rethrow) {
            throw exception
        }
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
    private fun onShutdown(error: Throwable?) {
        val cancellation = error.toCancellationException() ?: cancellationCause
        val cause = cancellation.cause ?: cancellation

        // Cancel and clear sideJobs
        sideJobsMap?.run {
            values.forEach { it.job?.cancel(cancellation) }
            clear()
        }

        // Cancel and clear input strategy
        intentStrategy.closeSafely()

        // Close and clear state & side effects machinery.
        value.closeSafely(cause)
        (sideEffectFlowField as? MutableSharedFlow)?.apply {
            val replayCache = replayCache
            @OptIn(ExperimentalCoroutinesApi::class)
            resetReplayCache()
            replayCache.forEach { it.closeSafely(cause) }
        }
        sideEffectChannel?.apply {
            while (true) {
                val result = tryReceive()
                if (result.isFailure) break // channel is empty or already closed
                result.getOrNull()?.closeSafely(cause)
            }
            close(cancellation.cause)
        }

        if (::decorator.isInitialized) {
            decorator.onClosed(cause)
        }
    }

    override fun toString(): String {
        val closeFlag = if (coroutineContext.isActive) "" else ";CLOSED"
        return "$F[$name$closeFlag]: $value"
    }

    private companion object {
        private const val F = "Fluxo"
        private val storeNumber = atomic(0)
    }

    // endregion


    // region StateFlow methods

    override val replayCache: List<State> get() = mutableState.replayCache

    override suspend fun collect(collector: FlowCollector<State>): Nothing {
        // Cancel the collecting Job if the store closed during the collection.
        val handle = currentCoroutineContext()[Job]?.dependOn(coroutineContext[Job])
        try {
            mutableState.collect(collector)
        } finally {
            // Always dispose the completion handle in the end
            handle?.dispose()
        }
    }

    // endregion
}
