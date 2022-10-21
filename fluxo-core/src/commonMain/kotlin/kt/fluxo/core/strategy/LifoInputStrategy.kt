package kt.fluxo.core.strategy

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.dsl.InputStrategyScope
import kt.fluxo.core.intercept.StoreRequest

// UI events oriented
internal object LifoInputStrategy : InputStrategy<Any?, Any?>() {

    override fun createQueue(): Channel<StoreRequest<Any?, Any?>> {
        return Channel(capacity = Channel.BUFFERED, BufferOverflow.DROP_LATEST)
    }

    override suspend fun InputStrategyScope<Any?, Any?>.processRequests(filteredQueue: Flow<StoreRequest<Any?, Any?>>) {
        filteredQueue.collectLatest(this)
    }
}
