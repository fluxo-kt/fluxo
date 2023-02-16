@file:Suppress("LongParameterList")

package kt.fluxo.core.internal

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kt.fluxo.core.SideJob
import kt.fluxo.core.dsl.BootstrapperScope
import kotlin.coroutines.CoroutineContext

internal class BootstrapperScopeImpl<in Intent, State, SideEffect : Any>(
    bootstrapper: Any?,
    guardian: InputStrategyGuardian?,
    getState: () -> State,
    updateStateAndGet: suspend ((State) -> State) -> State,
    private val emitIntent: suspend (Intent) -> Unit,
    private val sendIntent: (Intent) -> Job,
    sendSideEffect: suspend (SideEffect) -> Unit,
    sendSideJob: suspend (key: String, CoroutineContext, CoroutineStart, SideJob<Intent, State, SideEffect>) -> Job,
    subscriptionCount: StateFlow<Int>,
    coroutineContext: CoroutineContext,
) : StoreScopeImpl<Intent, State, SideEffect>(
    job = bootstrapper,
    guardian = guardian,
    getState = getState,
    updateStateAndGet = updateStateAndGet,
    sendSideEffect = sendSideEffect,
    sendSideJob = sendSideJob,
    subscriptionCount = subscriptionCount,
    coroutineContext = coroutineContext,
), BootstrapperScope<Intent, State, SideEffect> {

    override suspend fun emit(value: Intent) {
        guardian?.checkPostIntent()
        return emitIntent(value)
    }

    override fun send(intent: Intent): Job {
        guardian?.checkPostIntent()
        return sendIntent(intent)
    }
}
