package kt.fluxo.core.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.updateAndGet
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.dsl.SideJobScope
import kt.fluxo.core.dsl.StoreScope

internal class IntentHandlerScopeImpl<in Intent, State, SideEffect : Any>(
    private val guardian: InputStrategy.Guardian?,
    private val stateFlow: MutableStateFlow<State>,
    private val sendSideEffect: suspend (SideEffect) -> Unit,
    private val sendSideJob: (SideJobRequest<Intent, State, SideEffect>) -> Unit,
) : StoreScope<Intent, State, SideEffect> {

    override val state: State
        get() {
            guardian?.checkStateAccess()
            return stateFlow.value
        }

    override fun updateState(block: (State) -> State): State {
        guardian?.checkStateUpdate()
        return stateFlow.updateAndGet(block)
    }

    override fun getAndUpdateState(block: (State) -> State): State {
        guardian?.checkStateUpdate()
        return stateFlow.getAndUpdate(block)
    }

    override suspend fun postSideEffect(sideEffect: SideEffect) {
        guardian?.checkPostSideEffect()
        sendSideEffect(sideEffect)
    }

    override fun sideJob(key: String, block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit) {
        guardian?.checkSideJob()
        sendSideJob(SideJobRequest(key, block))
    }

    override fun noOp() {
        guardian?.checkNoOp()
    }

    internal fun close() {
        guardian?.close()
    }
}
