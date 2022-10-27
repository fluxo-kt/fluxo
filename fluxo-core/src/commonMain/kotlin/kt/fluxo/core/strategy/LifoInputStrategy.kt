package kt.fluxo.core.strategy

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.dsl.InputStrategyScope

/**
 * UI events oriented strategy. Cancels previous unfinished intents when receives new one.
 *
 * Provides more responsiveness, but can lose some intents!
 */
internal object LifoInputStrategy : InputStrategy() {

    override fun <Request> createQueue(): Channel<Request> {
        return Channel(capacity = Channel.CONFLATED)
    }

    override suspend fun <Request> (InputStrategyScope<Request>).processRequests(queue: Flow<Request>) {
        queue.collectLatest(this)
    }
}
