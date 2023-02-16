@file:Suppress("LongParameterList")

package kt.fluxo.core.internal

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kt.fluxo.core.SideJob
import kt.fluxo.core.dsl.StoreScope
import kotlin.coroutines.CoroutineContext

internal open class StoreScopeImpl<in Intent, State, SideEffect : Any>(
    internal val job: Any?,
    protected val guardian: InputStrategyGuardian?,
    private val getState: () -> State,
    private val updateStateAndGet: suspend ((State) -> State) -> State,
    private val sendSideEffect: suspend (SideEffect) -> Unit,
    private val sendSideJob: suspend (
        key: String,
        CoroutineContext,
        CoroutineStart,
        SideJob<Intent, State, SideEffect>,
    ) -> Job,
    final override val subscriptionCount: StateFlow<Int>,
    final override val coroutineContext: CoroutineContext,
) : StoreScope<Intent, State, SideEffect> {

    final override val state: State
        get() {
            guardian?.checkStateAccess()
            return getState()
        }

    final override suspend fun updateState(function: (State) -> State): State {
        guardian?.checkStateUpdate()
        return updateStateAndGet(function)
    }

    final override suspend fun postSideEffect(sideEffect: SideEffect) {
        guardian?.checkPostSideEffect()
        sendSideEffect(sideEffect)
    }

    final override suspend fun sideJob(
        key: String,
        context: CoroutineContext,
        start: CoroutineStart,
        block: SideJob<Intent, State, SideEffect>,
    ) {
        guardian?.checkSideJob()
        sendSideJob(key, context, start, block)
    }

    final override fun noOp() {
        guardian?.checkNoOp()
    }

    internal fun close() {
        guardian?.close()
    }
}
