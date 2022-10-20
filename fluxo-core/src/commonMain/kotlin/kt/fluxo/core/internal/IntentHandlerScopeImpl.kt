package kt.fluxo.core.internal

import kt.fluxo.core.InputStrategy
import kt.fluxo.core.dsl.SideJobScope
import kt.fluxo.core.dsl.StoreScope

internal class IntentHandlerScopeImpl<in Intent, State, SideEffect : Any>(
    private val guardian: InputStrategy.Guardian?,
    private val getState: () -> State,
    private val updateStateAndGet: ((State) -> State) -> State,
    private val sendSideEffect: suspend (SideEffect) -> Unit,
    private val sendSideJob: (SideJobRequest<Intent, State, SideEffect>) -> Unit,
) : StoreScope<Intent, State, SideEffect> {

    override val state: State
        get() {
            guardian?.checkStateAccess()
            return getState()
        }

    override fun updateState(block: (State) -> State): State {
        guardian?.checkStateUpdate()
        return updateStateAndGet(block)
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
