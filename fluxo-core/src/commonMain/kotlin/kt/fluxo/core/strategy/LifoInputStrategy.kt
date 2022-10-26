package kt.fluxo.core.strategy

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.dsl.InputStrategyScope
import kt.fluxo.core.intercept.StoreRequest

/**
 * UI events oriented strategy. Cancels previous unfinished intents when receives new one.
 *
 * Provides more responsiveness, but can lose some intents!
 */
internal object LifoInputStrategy : InputStrategy<Any?, Any?>() {

    override fun createQueue(): Channel<StoreRequest<Any?, Any?>> {
        return Channel(capacity = Channel.CONFLATED)
    }

    override suspend fun InputStrategyScope<Any?, Any?>.processRequests(filteredQueue: Flow<StoreRequest<Any?, Any?>>) {
        filteredQueue.collectLatest(this)
    }
}
