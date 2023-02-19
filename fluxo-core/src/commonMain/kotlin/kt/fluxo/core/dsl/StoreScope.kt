@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.dsl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kt.fluxo.core.IntentHandler
import kt.fluxo.core.SideJob
import kt.fluxo.core.Store
import kt.fluxo.core.annotation.CallSuper
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.annotation.ThreadSafe
import kt.fluxo.core.internal.RunningSideJob.Companion.DEFAULT_SIDE_JOB
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.internal.InlineOnly
import kotlin.js.JsName
import kotlin.jvm.JvmSynthetic

/**
 * Mutable [Store] scope for [handlers][IntentHandler], and so on.
 */
@ThreadSafe
@ExperimentalFluxoApi
public interface StoreScope<in Intent, State, in SideEffect : Any> : CoroutineScope {

    public val value: State

    /**
     * The number of subscribers (active collectors) for the current [Store].
     * Never negative and starts with zero. Can be used to react to changes in the number of subscriptions to this shared flow.
     *
     * @see kt.fluxo.core.repeatOnSubscription
     * @see kotlinx.coroutines.flow.MutableSharedFlow.subscriptionCount
     */
    public val subscriptionCount: StateFlow<Int>

    /**
     * Updates the [Store.state] atomically using the specified [function] of its value.
     *
     * [function] may be evaluated multiple times, if [Store.state] is being concurrently updated.
     *
     * @see MutableStateFlow.update
     */
    @CallSuper
    @JsName("updateState")
    public suspend fun updateState(function: (prevState: State) -> State): State

    /**
     * Dispatch a [sideEffect] through the [sideEffectFlow].
     *
     * @throws IllegalStateException if [sideEffect]s are off for this [Store]
     *
     * @TODO Better naming?
     */
    @CallSuper
    @JsName("postSideEffect")
    public suspend fun postSideEffect(sideEffect: SideEffect)

    /**
     * Do something other than [update the state][updateState] or [dispatch an effect][postSideEffect].
     * This is moving outside the strict MVI workflow, so make sure you know what you're doing with this.
     *
     * NOTE: best practice is to support [SideJob] cancellation!
     * For example, when deleting a record from the DB, do a soft deletion with possible restoration, if needed.
     *
     * NOTE: give a unique [key] for each distinct side-job within one [Store] to be sure they don't cancel each other!
     *
     * NOTE: side-jobs should support restartions.
     * Already-running side-jobs at the same [key] cancels when the new side-job starts.
     *
     * @see kotlinx.coroutines.launch
     */
    @CallSuper
    @JsName("sideJob")
    // FIXME: More options for a side job restarting or start-avoidance. SideJobs registry/management API
    public suspend fun sideJob(
        key: String = DEFAULT_SIDE_JOB,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: SideJob<Intent, State, SideEffect>,
    ): Job


    // region Migration and convenience helpers

    /**
     * Explicitly mark a branch in the [IntentHandler] as doing nothing.
     * It will be considered as been handled properly even though nothing happened.
     *
     * @TODO: Detekt/Lint checks for potentially errorneus usage of [StoreScope] as tested in guardian
     */
    public fun noOp() {}

    /** @see updateState */
    @InlineOnly
    @JvmSynthetic
    @JsName("reduce")
    @Deprecated(
        message = "Please use updateState extension instead",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("updateState(reducer)", "kt.fluxo.core.updateState"),
    )
    public suspend fun reduce(reducer: (State) -> State): State = updateState(reducer)


    /**
     * Use the [sideJob] function to launch long-running jobs in [StoreScope].
     *
     * @see sideJob
     * @see kotlinx.coroutines.launch
     */
    @InlineOnly
    @JvmSynthetic
    @JsName("launch")
    @Deprecated(
        message = "Use the sideJob function to launch long-running jobs in StoreScope",
        replaceWith = ReplaceWith("sideJob(key, context, start, block)"),
        level = DeprecationLevel.ERROR,
    )
    public fun launch(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        key: String = DEFAULT_SIDE_JOB,
        block: SideJob<Intent, *, *>,
    ): Job = throw NotImplementedError()

    /**
     * Use the [sideJob] function to launch long-running jobs in [StoreScope].
     *
     * @see sideJob
     * @see kotlinx.coroutines.launch
     */
    @InlineOnly
    @JvmSynthetic
    @JsName("async")
    @Deprecated(
        message = "Use the sideJob function to launch long-running jobs in StoreScope",
        replaceWith = ReplaceWith("sideJob(key, context, start, block)"),
        level = DeprecationLevel.ERROR,
    )
    public fun async(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        key: String = DEFAULT_SIDE_JOB,
        block: SideJob<Intent, *, *>,
    ): Unit = throw NotImplementedError()

    // endregion
}
