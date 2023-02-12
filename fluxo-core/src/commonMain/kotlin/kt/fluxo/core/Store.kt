@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kt.fluxo.core.annotation.CallSuper
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.annotation.ThreadSafe
import kt.fluxo.core.internal.Closeable
import kt.fluxo.core.internal.SideJobRequest.Companion.DEFAULT_SIDE_JOB
import kotlin.internal.InlineOnly
import kotlin.js.JsName
import kotlin.jvm.JvmSynthetic

/**
 * The core of the Fluxo MVI.
 * Represents a state store with its input and outputs.
 *
 * [Store] implements [StateFlow] and [FlowCollector] interfaces.
 * It allows you to easily combine different [Store]s with each other.
 * Also [Store] is a [CoroutineScope], so you can integrate it with any existing coroutine workflow.
 * Finally, [Store] is a [Closeable] as it can help to consume it in JVM environment.
 *
 * Tip: Use [isActive][kotlinx.coroutines.isActive] to check if store is active (not closed).
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
     * [Store] name. Auto-generated or specified via [FluxoSettings.name].
     */
    public val name: String


    /**
     * [StateFlow] with the number of subscribers (active collectors) for the current [Store].
     * Never negative and starts with zero. Can be used to react to changes in the number of subscriptions to this [Store].
     *
     * @see kt.fluxo.core.repeatOnSubscription
     * @see kotlinx.coroutines.flow.MutableSharedFlow.subscriptionCount
     */
    @ExperimentalFluxoApi
    public val subscriptionCount: StateFlow<Int>


    /**
     * Queues an [intent] for processing.
     * Prefer to use [emit] if possible – it avoids the new [CoroutineScope] creation as it's a `suspend` function.
     *
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
     * This method provides compatibility with [FlowCollector], allowing the store to be connected to any flow of intents.
     *
     * @throws FluxoClosedException
     */
    @CallSuper
    public override suspend fun emit(value: Intent)


    /**
     * Starts [Store] processing if not started already.
     *
     * @throws FluxoClosedException
     */
    @CallSuper
    @JsName("start")
    public fun start(): Job?

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


    @InlineOnly
    @JsName("launch")
    @Deprecated(
        message = "Please use the sideJob function to launch long running jobs in Fluxo",
        replaceWith = ReplaceWith("sideJob(key, block)"),
        level = DeprecationLevel.ERROR,
    )
    public fun launch(key: String = DEFAULT_SIDE_JOB, block: SideJob<Intent, *, *>): Unit = throw NotImplementedError()

    @InlineOnly
    @JsName("async")
    @Deprecated(
        message = "Please use the sideJob function to launch long running jobs in Fluxo",
        replaceWith = ReplaceWith("sideJob(key, block)"),
        level = DeprecationLevel.ERROR,
    )
    public fun async(key: String = DEFAULT_SIDE_JOB, block: SideJob<Intent, *, *>): Unit = throw NotImplementedError()

    // endregion
}
