@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.dsl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kt.fluxo.core.Store
import kt.fluxo.core.annotation.FluxoDsl
import kt.fluxo.core.annotation.InternalFluxoApi
import kotlin.internal.InlineOnly

@FluxoDsl
@InternalFluxoApi
public interface StoreScope<in Intent, State, in SideEffect : Any> : CoroutineScope {

    private companion object {
        private const val DEFAULT_SIDE_JOB = "default"
        private const val DEFAULT_REPEAT_ON_SUBSCRIPTION_JOB = "repeatOnSubscription"
    }


    public val state: State

    /**
     * The number of subscribers (active collectors) for the current [Store].
     * Never negative and starts with zero. Can be used to react to changes in the number of subscriptions to this shared flow.
     *
     * @see kt.fluxo.core.repeatOnSubscription
     * @see kotlinx.coroutines.flow.MutableSharedFlow.subscriptionCount
     */
    public val subscriptionCount: StateFlow<Int>

    public suspend fun updateState(block: (State) -> State): State

    public suspend fun postSideEffect(sideEffect: SideEffect)

    public suspend fun sideJob(key: String = DEFAULT_SIDE_JOB, block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit)

    public fun noOp()


    // region Convenience helpers

    @InlineOnly
    @Deprecated(
        message = "Please use the sideJob function to launch long running jobs",
        replaceWith = ReplaceWith("sideJob(key, block)"),
        level = DeprecationLevel.ERROR,
    )
    public fun launch(key: String = DEFAULT_SIDE_JOB, block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit): Unit =
        throw NotImplementedError()

    @InlineOnly
    @Deprecated(
        message = "Please use the sideJob function to launch long running jobs",
        replaceWith = ReplaceWith("sideJob(key, block)"),
        level = DeprecationLevel.ERROR,
    )
    public fun async(key: String = DEFAULT_SIDE_JOB, block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit): Unit =
        throw NotImplementedError()

    // endregion


    // region Migration helpers

    /**
     * Helper for migration from Orbit.
     * Consider to use [updateState] instead.
     */
    @InlineOnly
    @Deprecated(
        message = "Please use the updateState instead",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("updateState { state ->\n    reducer()    \n}"),
    )
    public suspend fun reduce(reducer: (State) -> State) {
        updateState(reducer)
    }

    // endregion
}
