package kt.fluxo.core.factory

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kt.fluxo.core.Bootstrapper
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.SideJob
import kt.fluxo.core.annotation.CallSuper
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kotlin.coroutines.CoroutineContext

/**
 * Convenience implementation base for the [StoreDecorator].
 *
 * @TODO All basic tests with this wrapper used
 */
@ExperimentalFluxoApi
@Suppress("UnnecessaryAbstractClass", "TooManyFunctions")
public abstract class StoreDecoratorBase<Intent, State, SideEffect : Any>(
    private val store: StoreDecorator<Intent, State, SideEffect>,
) : StoreDecorator<Intent, State, SideEffect> {

    // Delegate manually instead of using the kotlin `by` automation to reduce the public API surface.

    override var value: State
        @CallSuper get() = store.value
        @CallSuper set(value) {
            store.value = value
        }

    override val name: String @CallSuper get() = store.name

    override fun toString(): String = store.toString()

    final override val sideEffectFlow: Flow<SideEffect> @CallSuper get() = store.sideEffectFlow

    final override val replayCache: List<State> @CallSuper get() = store.replayCache

    final override val coroutineContext: CoroutineContext @CallSuper get() = store.coroutineContext

    @ExperimentalFluxoApi
    final override val subscriptionCount: StateFlow<Int> @CallSuper get() = store.subscriptionCount


    @CallSuper
    final override fun init(decorator: StoreDecorator<Intent, State, SideEffect>, conf: FluxoSettings<Intent, State, SideEffect>): Unit =
        store.init(decorator, conf)

    @CallSuper
    override fun start(): Job = store.start()

    @CallSuper
    override suspend fun onStart(bootstrapper: Bootstrapper<Intent, State, SideEffect>?): Unit = store.onStart(bootstrapper)

    @CallSuper
    override suspend fun onBootstrap(bootstrapper: Bootstrapper<Intent, State, SideEffect>): Unit = store.onBootstrap(bootstrapper)


    @CallSuper
    @ExperimentalFluxoApi
    override fun compareAndSet(expect: State, update: State): Boolean = store.compareAndSet(expect, update)

    @CallSuper
    override fun onStateChanged(state: State, fromState: State): Unit = store.onStateChanged(state, fromState)

    @CallSuper
    override suspend fun emit(value: Intent): Unit = store.emit(value)

    @CallSuper
    override fun send(intent: Intent): Job = store.send(intent)

    @CallSuper
    override suspend fun onIntent(intent: Intent): Unit = store.onIntent(intent)

    @CallSuper
    override fun onUndeliveredIntent(intent: Intent, wasResent: Boolean): Unit = store.onUndeliveredIntent(intent, wasResent)


    @CallSuper
    override suspend fun postSideEffect(sideEffect: SideEffect): Unit = store.postSideEffect(sideEffect)

    @CallSuper
    override fun onUndeliveredSideEffect(sideEffect: SideEffect, wasResent: Boolean): Unit =
        store.onUndeliveredSideEffect(sideEffect, wasResent)


    @CallSuper
    override suspend fun sideJob(
        key: String,
        context: CoroutineContext,
        start: CoroutineStart,
        onError: ((error: Throwable) -> Unit)?,
        block: SideJob<Intent, State, SideEffect>,
    ): Job = store.sideJob(key = key, context = context, start = start, onError = onError, block = block)

    @CallSuper
    override suspend fun onSideJob(key: String, wasRestarted: Boolean, sideJob: SideJob<Intent, State, SideEffect>): Unit =
        store.onSideJob(key = key, wasRestarted = wasRestarted, sideJob = sideJob)


    @CallSuper
    override fun onUnhandledError(error: Throwable): Boolean = store.onUnhandledError(error)

    @CallSuper
    override fun close(): Unit = store.close()

    @CallSuper
    override fun onClosed(cause: Throwable?): Unit = store.onClosed(cause)

    @CallSuper
    override suspend fun collect(collector: FlowCollector<State>): Nothing = store.collect(collector)
}
