@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.dsl

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.internal.value
import kt.fluxo.common.annotation.ExperimentalFluxoApi
import kt.fluxo.core.IntentHandler
import kt.fluxo.core.SideJob
import kt.fluxo.core.Store
import kt.fluxo.core.StoreSE
import kt.fluxo.core.annotation.CallSuper
import kt.fluxo.core.annotation.FluxoDsl
import kt.fluxo.core.annotation.ThreadSafe
import kt.fluxo.core.factory.StoreDecorator
import kt.fluxo.core.internal.RunningSideJob.Companion.DEFAULT_SIDE_JOB
import kt.fluxo.core.updateState
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.internal.LowPriorityInOverloadResolution
import kotlin.js.JsName
import kotlin.jvm.JvmSynthetic

/**
 * Mutable [Store] scope for [handlers][IntentHandler], [decorators][StoreDecorator], and so on.
 */
@FluxoDsl
@ThreadSafe
@ExperimentalFluxoApi
public interface StoreScope<in Intent, State, SideEffect : Any> : StoreSE<Intent, State, SideEffect> {

    override var value: State

    /**
     * Atomically compares the current [value] with [expect] and sets it to [update]
     * if it is equal to [expect].
     * The result is `true` if the [value] was set to [update] and `false` otherwise.
     *
     * Uses a regular comparison using [Any.equals].
     * If both [expect] and [update] are equal to the current [value], this function returns `true`,
     * but it doesn't actually change the reference stored in the [value].
     *
     * See the [updateState][kt.fluxo.core.updateState] for convenient usage.
     *
     * @see kt.fluxo.core.updateState
     * @see MutableStateFlow.compareAndSet
     * @see MutableStateFlow.update
     */
    @CallSuper
    @JsName("compareAndSet")
    public fun compareAndSet(expect: State, update: State): Boolean

    /**
     * Dispatch a [sideEffect] through the [sideEffectFlow].
     *
     * @throws IllegalStateException if [sideEffect]s are off for this [Store]
     *
     * @TODO Throw only when debugChecks enabled to better support iOS and functional programming overall?
     * @TODO Better naming?
     */
    @CallSuper
    @JsName("postSideEffect")
    public suspend fun postSideEffect(sideEffect: SideEffect)

    /**
     * Do something other than [update the state][kt.fluxo.core.updateState] or [dispatch an effect][postSideEffect].
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
        onError: ((error: Throwable) -> Unit)? = null,
        block: SideJob<Intent, State, SideEffect>,
    ): Job


    // region Migration and convenience helpers

    /**
     * Explicitly mark a branch in the [IntentHandler] as doing nothing.
     * Marks as correctly handled even though nothing happened.
     *
     * @TODO: Detekt/Lint checks for potentially errorneus usage of [StoreScope] as tested in guardian
     */
    public fun noOp() {}


    /** @see kt.fluxo.core.updateState */
    @JsName("updateState")
    @LowPriorityInOverloadResolution
    @Deprecated(
        message = "Please use updateState extension instead",
        replaceWith = ReplaceWith("updateState(reducer)", "kt.fluxo.core.updateState"),
        level = DeprecationLevel.WARNING,
    )
    public fun updateState(reducer: (State) -> State): State = updateState(reducer)

    /** @see kt.fluxo.core.updateState */
    @JsName("reduce")
    @Deprecated(
        message = "Please use updateState extension instead",
        replaceWith = ReplaceWith("updateState(reducer)", "kt.fluxo.core.updateState"),
        level = DeprecationLevel.WARNING,
    )
    public fun reduce(reducer: (State) -> State): State = updateState(reducer)


    /**
     * Use the [sideJob] function to launch long-running jobs in the [StoreScope].
     *
     * @see sideJob
     * @see kotlinx.coroutines.launch
     */
    @JvmSynthetic
    @Deprecated(
        message = "Use the sideJob function to launch long-running jobs in the StoreScope",
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
     * Use the [sideJob] function to launch long-running jobs in the [StoreScope].
     *
     * @see sideJob
     * @see kotlinx.coroutines.launch
     */
    @JvmSynthetic
    @Deprecated(
        message = "Use the sideJob function to launch long-running jobs in the StoreScope",
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
