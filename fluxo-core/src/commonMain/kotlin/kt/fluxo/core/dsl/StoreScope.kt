package kt.fluxo.core.dsl

import kt.fluxo.core.annotation.FluxoDsl

@FluxoDsl
public interface StoreScope<in Intent, State, in SideEffect : Any> {

    public val state: State

    public fun updateState(block: (State) -> State): State

    public suspend fun postSideEffect(sideEffect: SideEffect)

    public fun sideJob(key: String, block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit)

    public fun noOp()


    // region Convinience helpers

    @Deprecated("Please use the sideJob function to launch long running jobs", ReplaceWith("sideJob(key, block)"))
    public fun launch(key: String, block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit): Unit = sideJob(key, block)

    @Deprecated("Please use the sideJob function to launch long running jobs", ReplaceWith("sideJob(key, block)"))
    public fun async(key: String, block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit): Unit = sideJob(key, block)

    // endregion
}
