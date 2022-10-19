package kt.fluxo.core.strategy

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.StoreRequest
import kt.fluxo.core.dsl.InputStrategyScope

// no guarantee that inputs will be processed in any given order
internal object ParallelInputStrategy : InputStrategy<Any?, Any?>() {

    override val rollbackOnCancellation: Boolean get() = false

    override suspend fun InputStrategyScope<Any?, Any?>.processInputs(filteredQueue: Flow<StoreRequest<Any?, Any?>>) {
        coroutineScope {
            val scope = this
            filteredQueue.collect { queued ->
                scope.launch {
                    invoke(queued, Guardian(parallelProcessing = true))
                }
            }
        }
    }
}
