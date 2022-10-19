package kt.fluxo.core.internal

import kotlinx.coroutines.CoroutineScope
import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.dsl.SideJobScope

@InternalFluxoApi
internal class SideJobScopeImpl<in Intent, out State, in SideEffect : Any>(
    private val sendIntentToStore: suspend (Intent) -> Unit,
    private val sendSideEffectToStore: suspend (SideEffect) -> Unit,
    override val currentStateWhenStarted: State,
    override val restartState: SideJobScope.RestartState,
    private val coroutineScope: CoroutineScope,
) : SideJobScope<Intent, State, SideEffect>, CoroutineScope by coroutineScope {

    override suspend fun postIntent(intent: Intent) = sendIntentToStore(intent)

    override suspend fun postSideEffect(sideEffect: SideEffect) = sendSideEffectToStore(sideEffect)
}
