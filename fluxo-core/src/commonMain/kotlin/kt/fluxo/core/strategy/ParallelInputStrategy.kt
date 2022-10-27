package kt.fluxo.core.strategy

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.dsl.InputStrategyScope

/** Parallel processing of all intents. No guarantee that inputs will be processed in any given order. */
internal object ParallelInputStrategy : InputStrategy() {

    override val parallelProcessing: Boolean get() = true

    override suspend fun <Request> (InputStrategyScope<Request>).processRequests(queue: Flow<Request>) {
        coroutineScope {
            queue.collect { request ->
                launch {
                    invoke(request)
                }
            }
        }
    }
}
