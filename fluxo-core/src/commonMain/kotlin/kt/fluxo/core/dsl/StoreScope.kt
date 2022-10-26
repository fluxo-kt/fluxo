@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.dsl

import kt.fluxo.core.annotation.FluxoDsl
import kotlin.internal.InlineOnly

@FluxoDsl
public interface StoreScope<in Intent, State, in SideEffect : Any> {

    private companion object {
        private const val DEFAULT_SIDE_JOB = "default"
    }


    public val state: State

    public fun updateState(block: (State) -> State): State

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
     * Helper for migration from Orbit
     */
    @InlineOnly
    @Deprecated(
        message = "Please use the updateState instead",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("updateState { state ->\n    reducer()    \n}"),
    )
    public fun reduce(reducer: Any.() -> State): Unit = throw NotImplementedError()

    // endregion
}
