package kt.fluxo.core.strategy

import kotlinx.coroutines.flow.Flow
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.dsl.InputStrategyScope
import kt.fluxo.core.intercept.StoreRequest

// background processing oriented strategy
internal object FifoInputStrategy : InputStrategy<Any?, Any?>() {

    override suspend fun InputStrategyScope<Any?, Any?>.processRequests(filteredQueue: Flow<StoreRequest<Any?, Any?>>) {
        filteredQueue.collect(this)
    }
}
