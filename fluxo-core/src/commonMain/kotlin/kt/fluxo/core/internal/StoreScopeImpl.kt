package kt.fluxo.core.internal

import kt.fluxo.core.dsl.SideJobScope
import kt.fluxo.core.dsl.StoreScope

internal open class StoreScopeImpl<in Intent, State, SideEffect : Any>(
    internal val job: Any?,
    private val guardian: InputStrategyGuardian?,
    private val getState: () -> State,
    private val updateStateAndGet: ((State) -> State) -> State,
    private val sendSideEffect: suspend (SideEffect) -> Unit,
    private val sendSideJob: suspend (SideJobRequest<Intent, State, SideEffect>) -> Unit,
) : StoreScope<Intent, State, SideEffect> {

    final override val state: State
        get() {
            guardian?.checkStateAccess()
            return getState()
        }

    final override fun updateState(block: (State) -> State): State {
        guardian?.checkStateUpdate()
        return updateStateAndGet(block)
    }

    final override suspend fun postSideEffect(sideEffect: SideEffect) {
        guardian?.checkPostSideEffect()
        sendSideEffect(sideEffect)
    }

    final override suspend fun sideJob(key: String, block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit) {
        guardian?.checkSideJob()
        sendSideJob(SideJobRequest(key, job, block))
    }

    final override fun noOp() {
        guardian?.checkNoOp()
    }

    internal fun close() {
        guardian?.close()
    }
}
