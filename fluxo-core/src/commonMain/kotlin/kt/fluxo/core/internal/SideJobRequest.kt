package kt.fluxo.core.internal

import kt.fluxo.core.dsl.SideJobScope

internal class SideJobRequest<out Intent, State, out SideEffect : Any>(
    val key: String,
    val parent: Any?,
    val block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit,
)
