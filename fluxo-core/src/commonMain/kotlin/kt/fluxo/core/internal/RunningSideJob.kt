package kt.fluxo.core.internal

import kotlinx.coroutines.Job
import kotlin.jvm.Volatile

internal class RunningSideJob<out Intent, State, SideEffect : Any>(
    internal val request: SideJobRequest<Intent, State, SideEffect>,
    @Volatile
    internal var job: Job?,
)
