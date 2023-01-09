package kt.fluxo.core.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.dsl.SideJobScope

@InternalFluxoApi
@Suppress("LongParameterList")
internal class SideJobScopeImpl<in Intent, State, in SideEffect : Any>(
    private val updateStateAndGet: suspend ((State) -> State) -> State,
    private val emitIntent: suspend (Intent) -> Unit,
    private val sendIntent: (Intent) -> Job,
    private val sendSideEffect: suspend (SideEffect) -> Unit,
    override val currentStateWhenStarted: State,
    override val restartState: SideJobScope.RestartState,
    coroutineScope: CoroutineScope,
) : SideJobScope<Intent, State, SideEffect>, CoroutineScope by coroutineScope {

    override suspend fun updateState(function: (State) -> State) = updateStateAndGet(function)

    override suspend fun emit(value: Intent) = emitIntent(value)

    override fun send(intent: Intent): Job = sendIntent(intent)

    override suspend fun postSideEffect(sideEffect: SideEffect) = sendSideEffect(sideEffect)
}
