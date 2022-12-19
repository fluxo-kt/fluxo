package kt.fluxo.core.strategy

import kotlinx.coroutines.flow.Flow
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.dsl.InputStrategyScope

/**
 * `First-in, first-out` - ordered processing strategy. Predictable and intuitive, default choice.
 *
 * Consider [Parallel][ParallelInputStrategy] or [Lifo][LifoInputStrategy] instead if you need more responsiveness.
 */
internal object FifoInputStrategy : InputStrategy() {

    override suspend fun <Request> (InputStrategyScope<Request>).processRequests(queue: Flow<Request>) {
        queue.collect(this)
    }
}
