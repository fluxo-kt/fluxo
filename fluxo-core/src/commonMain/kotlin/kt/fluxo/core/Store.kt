@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kt.fluxo.common.annotation.ExperimentalFluxoApi
import kt.fluxo.common.annotation.InlineOnly
import kt.fluxo.core.annotation.CallSuper
import kt.fluxo.core.annotation.ThreadSafe
import kt.fluxo.core.internal.Closeable
import kotlin.js.JsName
import kotlin.jvm.JvmSynthetic

/**
 * The core of the Fluxo MVI.
 * Represents a state node with its inputs (intents) and outputs (state updates).
 *
 * [Store] implements [StateFlow] and [FlowCollector] interfaces,
 * allowing to combine different [Store]s with each other and with other [Flow]s.
 * Also [Store] is a [CoroutineScope], so you can integrate it with any existing coroutine workflow.
 * Finally, [Store] is a [Closeable] as it can help to consume it in JVM environment.
 *
 * Tip: use [isActive][kotlinx.coroutines.isActive] to get if store is active (not closed).
 *
 * @param Intent Intent type for this [Store]. See [Container] for MVVM+ [Store].
 * @param State State type for this [Store].
 *
 * @see StoreSE for the state store with side effects
 * @see Container for the state store with side effects
 */
@ThreadSafe
// @SubclassOptInRequired(ExperimentalFluxoApi::class) // TODO: Kotlin API version 1.8
public interface Store<in Intent, out State> : StateFlow<State>, FlowCollector<Intent>, CoroutineScope, Closeable {

    /**
     * [Store] name. Autogenerated or specified via [FluxoSettings.name].
     */
    public val name: String


    /**
     * [StateFlow] with the number of subscribers (active collectors) for the current [Store].
     * Never negative and starts with zero.
     * Use to react to changes in the number of subscriptions to this [Store].
     *
     * @see kt.fluxo.core.repeatOnSubscription
     * @see kotlinx.coroutines.flow.MutableSharedFlow.subscriptionCount
     */
    @ExperimentalFluxoApi
    public val subscriptionCount: StateFlow<Int>


    /**
     * Queues an [intent] for processing.
     * Prefer to use [emit] if possible – it avoids the new [CoroutineScope] creation as it is a `suspend` function.
     *
     * @return a [Job] that can be [joined][Job.join] to await for the full [intent] processing completion.
     *  Sometimes it is useful but not recommended!
     *  It is always dangerous and can lead to deadlocks.
     *
     * @throws FluxoClosedException
     *
     * @TODO Return kotlin.Result instead of throwing to better support iOS and functional programming overall
     * @TODO Lint/Detekt rules with recommendation to use [emit] over [send]/intent when called in suspend environment
     *   and returned value is not used.
     *   Also, returned Job usage can be checked in production code and discouraged.
     */
    @CallSuper
    @JsName("send")
    @Throws(FluxoClosedException::class)
    public fun send(intent: Intent): Job

    /**
     * Queues an [intent][value] for processing.
     *
     * This method provides compatibility with [FlowCollector],
     * allowing the store to connect to any flow of [Intent]s.
     *
     * @throws FluxoClosedException
     *
     * @TODO Throw only when debugChecks enabled to better support iOS and functional programming overall
     */
    @CallSuper
    public override suspend fun emit(value: Intent)


    /**
     * Starts [Store] processing if not started already.
     *
     * @throws FluxoClosedException
     *
     * @TODO Return kotlin.Result instead of throwing to better support iOS and functional programming overall
     */
    @CallSuper
    @JsName("start")
    public fun start(): Job

    /**
     * Closes this [Store], cancelling its [Job] and all its children.
     *
     * @see kotlinx.coroutines.cancel
     */
    @CallSuper
    public override fun close(): Unit = cancel()


    // region Migration helpers

    /**
     * Reactive access to the [State].
     *
     * @see state
     */
    @InlineOnly
    @get:JvmSynthetic
    @Deprecated("Use store itself", ReplaceWith("this"), level = DeprecationLevel.ERROR)
    public val stateFlow: StateFlow<State> get() = this

    /**
     * Current [State]. Shortcut for a `stateFlow.value`.
     *
     * @see value
     * @see stateFlow
     */
    @InlineOnly
    @get:JvmSynthetic
    @Deprecated("Use value directly", ReplaceWith("value"), level = DeprecationLevel.ERROR)
    public val state: State get() = value

    // endregion
}
