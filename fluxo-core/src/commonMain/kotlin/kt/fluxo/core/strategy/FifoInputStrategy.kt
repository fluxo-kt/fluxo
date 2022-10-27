package kt.fluxo.core.strategy

import kotlinx.coroutines.flow.Flow
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.dsl.InputStrategyScope

/**
 * Ordered processing strategy. Predictable and intuitive. Best for background.
 * Consider [LifoInputStrategy] for the UI or more responsiveness instead.
 */
internal object FifoInputStrategy : InputStrategy() {

    override suspend fun <Request> (InputStrategyScope<Request>).processRequests(queue: Flow<Request>) {
        queue.collect(this)
    }
}
