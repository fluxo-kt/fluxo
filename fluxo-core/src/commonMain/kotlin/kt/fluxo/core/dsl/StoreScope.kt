package kt.fluxo.core.dsl

import kt.fluxo.core.annotation.FluxoDsl

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


    // region Convinience helpers

    @Deprecated(
        message = "Please use the sideJob function to launch long running jobs",
        replaceWith = ReplaceWith("sideJob(key, block)"),
        level = DeprecationLevel.ERROR,
    )
    public fun launch(key: String = DEFAULT_SIDE_JOB, block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit): Unit =
        throw NotImplementedError()

    @Deprecated(
        message = "Please use the sideJob function to launch long running jobs",
        replaceWith = ReplaceWith("sideJob(key, block)"),
        level = DeprecationLevel.ERROR,
    )
    public fun async(key: String = DEFAULT_SIDE_JOB, block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit): Unit =
        throw NotImplementedError()

    // endregion
}
