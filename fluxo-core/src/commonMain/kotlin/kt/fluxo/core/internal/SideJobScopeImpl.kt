package kt.fluxo.core.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.dsl.SideJobScope

@InternalFluxoApi
internal class SideJobScopeImpl<in Intent, out State, in SideEffect : Any>(
    private val sendIntent: suspend (Intent) -> Deferred<Unit>,
    private val sendSideEffect: suspend (SideEffect) -> Unit,
    override val currentStateWhenStarted: State,
    override val restartState: SideJobScope.RestartState,
    private val coroutineScope: CoroutineScope,
) : SideJobScope<Intent, State, SideEffect>, CoroutineScope by coroutineScope {

    override suspend fun postIntent(intent: Intent) = sendIntent(intent)

    override suspend fun postSideEffect(sideEffect: SideEffect) = sendSideEffect(sideEffect)
}
