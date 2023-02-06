package kt.fluxo.core

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.annotation.ThreadSafe
import kt.fluxo.core.intercept.FluxoEvent
import kt.fluxo.core.internal.Closeable
import kotlin.coroutines.cancellation.CancellationException
import kotlin.js.JsName


/**
 * Convenience typealias for an MVVM+ Fluxo [Store] setup.
 */
public typealias Container<State, SideEffect> = Store<FluxoIntent<State, SideEffect>, State, SideEffect>

/**
 * Convenience typealias for an MVVM+ Fluxo [Store] setup with side effects disabled.
 */
public typealias ContainerS<State> = Container<State, Nothing>

/**
 * Convenience typealias for a Fluxo [Store] setup with side effects disabled.
 */
public typealias StoreS<Intent, State> = Store<Intent, State, Nothing>


/**
 * Fluxo state store.
 *
 * Tip: Use [isActive][kotlinx.coroutines.isActive] to check if store is active.
 */
@ThreadSafe
public interface Store<Intent, State, SideEffect : Any> : Closeable {

    /**
     * [Store] name. Auto-generated or specified via [FluxoSettings.name].
     *
     * @see state
     */
    public val name: String


    /**
     * Reactive access to the [State].
     *
     * @see state
     */
    public val stateFlow: StateFlow<State>

    /**
     * Current [State]. Shortcut for a `stateFlow.value`.
     *
     * @see stateFlow
     */
    public val state: State get() = stateFlow.value


    /**
     * A _hot_ [Flow] that shares emitted [SideEffect]s among its collectors.
     *
     * Behavior of this flow can be configured with [FluxoSettings.sideEffectsStrategy].
     *
     * @see FluxoSettings.sideEffectsStrategy
     * @see SideEffectsStrategy
     *
     * @throws IllegalStateException if [SideEffect]s where disabled for this [Store].
     */
    public val sideEffectFlow: Flow<SideEffect>


    /**
     * [StateFlow] with the number of subscribers (active collectors) for the current [Store].
     * Never negative and starts with zero. Can be used to react to changes in the number of subscriptions to this [Store].
     *
     * @see kt.fluxo.core.repeatOnSubscription
     * @see kotlinx.coroutines.flow.MutableSharedFlow.subscriptionCount
     */
    @ExperimentalFluxoApi
    public val subscriptionCount: StateFlow<Int>

    @ExperimentalFluxoApi
    public val eventsFlow: Flow<FluxoEvent<Intent, State, SideEffect>>

    public val isActive: Boolean


    /**
     * Queues an [intent] for processing.
     *
     * @return a [Job] that can be [joined][Job.join] to await for processing completion
     *
     * @throws FluxoClosedException
     */
    @JsName("send")
    @Throws(FluxoClosedException::class)
    public fun send(intent: Intent)

    /**
     * Queues an [intent] for processing.
     *
     * @return a [Job] that can be [joined][Job.join] to await for processing completion
     *
     * @throws FluxoClosedException
     */
    @JsName("sendAsync")
    @Throws(FluxoClosedException::class, CancellationException::class)
    public suspend fun sendAsync(intent: Intent): Job


    /**
     * Starts [Store] processing if not started already.
     *
     * @throws FluxoClosedException
     */
    @JsName("start")
    public fun start(): Job?

    public override fun close()
}
