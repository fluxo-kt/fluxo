package kt.fluxo.core.strategy

import kotlinx.coroutines.flow.Flow
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.dsl.InputStrategyScope
import kt.fluxo.core.intercept.StoreRequest

/**
 * Ordered processing strategy. Predictable and intuitive. Best for background.
 * Consider [LifoInputStrategy] for the UI or more responsiveness instead.
 */
internal object FifoInputStrategy : InputStrategy<Any?, Any?>() {

    override suspend fun InputStrategyScope<Any?, Any?>.processRequests(filteredQueue: Flow<StoreRequest<Any?, Any?>>) {
        filteredQueue.collect(this)
    }
}
