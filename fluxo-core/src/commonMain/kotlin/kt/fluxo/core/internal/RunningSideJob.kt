package kt.fluxo.core.internal

import kotlinx.coroutines.Job
import kt.fluxo.core.dsl.SideJobScope
import kotlin.jvm.Volatile

internal class RunningSideJob<out Intent, in State, SideEffect : Any>(
    internal val key: String,
    internal val block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit,
    @Volatile
    internal var job: Job?,
)
