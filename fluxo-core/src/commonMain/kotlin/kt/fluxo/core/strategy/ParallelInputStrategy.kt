package kt.fluxo.core.strategy

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.dsl.InputStrategyScope
import kt.fluxo.core.intercept.StoreRequest

/** Parallel processing of all intents. No guarantee that inputs will be processed in any given order. */
internal object ParallelInputStrategy : InputStrategy<Any?, Any?>() {

    override val parallelProcessing: Boolean get() = true

    override suspend fun InputStrategyScope<Any?, Any?>.processRequests(filteredQueue: Flow<StoreRequest<Any?, Any?>>) {
        coroutineScope {
            filteredQueue.collect { request ->
                launch {
                    invoke(request)
                }
            }
        }
    }
}
