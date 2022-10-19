package kt.fluxo.core.internal

import kt.fluxo.core.dsl.SideJobScope

internal class SideJobRequest<out Intent, in State, out SideEffect : Any>(
    val key: String,
    val block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit,
)
