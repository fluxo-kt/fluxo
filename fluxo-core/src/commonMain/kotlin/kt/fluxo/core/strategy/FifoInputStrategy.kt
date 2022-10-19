package kt.fluxo.core.strategy

import kotlinx.coroutines.flow.Flow
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.StoreRequest
import kt.fluxo.core.dsl.InputStrategyScope

// background processing oriented strategy
internal object FifoInputStrategy : InputStrategy<Any?, Any?>() {

    override suspend fun InputStrategyScope<Any?, Any?>.processInputs(filteredQueue: Flow<StoreRequest<Any?, Any?>>) {
        filteredQueue.collect { queued ->
            invoke(queued, Guardian())
        }
    }
}
