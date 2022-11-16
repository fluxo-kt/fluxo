@file:Suppress("LongParameterList")

package kt.fluxo.core.internal

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.StateFlow
import kt.fluxo.core.dsl.BootstrapperScope

internal class BootstrapperScopeImpl<in Intent, State, SideEffect : Any>(
    bootstrapper: Any?,
    guardian: InputStrategyGuardian?,
    getState: () -> State,
    updateStateAndGet: suspend ((State) -> State) -> State,
    private val sendIntent: suspend (Intent) -> Deferred<Unit>,
    sendSideEffect: suspend (SideEffect) -> Unit,
    sendSideJob: suspend (SideJobRequest<Intent, State, SideEffect>) -> Unit,
    subscriptionCount: StateFlow<Int>
) : StoreScopeImpl<Intent, State, SideEffect>(
    job = bootstrapper,
    guardian = guardian,
    getState = getState,
    updateStateAndGet = updateStateAndGet,
    sendSideEffect = sendSideEffect,
    sendSideJob = sendSideJob,
    subscriptionCount = subscriptionCount,
), BootstrapperScope<Intent, State, SideEffect> {

    override suspend fun postIntent(intent: Intent) = sendIntent(intent)
}
