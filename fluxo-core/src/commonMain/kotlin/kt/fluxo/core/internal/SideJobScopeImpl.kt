package kt.fluxo.core.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.dsl.SideJobScope

@InternalFluxoApi
internal class SideJobScopeImpl<in Intent, State, in SideEffect : Any>(
    private val updateStateAndGet: suspend ((State) -> State) -> State,
    private val sendIntent: suspend (Intent) -> Job,
    private val sendSideEffect: suspend (SideEffect) -> Unit,
    override val currentStateWhenStarted: State,
    override val restartState: SideJobScope.RestartState,
    coroutineScope: CoroutineScope,
) : SideJobScope<Intent, State, SideEffect>, CoroutineScope by coroutineScope {

    override suspend fun updateState(block: (State) -> State) = updateStateAndGet(block)

    override suspend fun postIntent(intent: Intent) = sendIntent(intent)

    override suspend fun postSideEffect(sideEffect: SideEffect) = sendSideEffect(sideEffect)
}
