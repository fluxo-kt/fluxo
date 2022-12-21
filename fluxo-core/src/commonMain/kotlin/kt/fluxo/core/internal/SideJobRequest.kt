package kt.fluxo.core.internal

import kt.fluxo.core.SideJob

internal class SideJobRequest<out Intent, State, out SideEffect : Any>(
    val key: String,
    val parent: Any?,
    val block: SideJob<Intent, State, SideEffect>,
)
