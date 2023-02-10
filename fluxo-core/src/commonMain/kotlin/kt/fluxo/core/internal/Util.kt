@file:Suppress("KDocUnresolvedReference")

package kt.fluxo.core.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kt.fluxo.core.annotation.ExperimentalFluxoApi

internal fun Throwable?.toCancellationException() = when (this) {
    null -> null
    is CancellationException -> this
    else -> CancellationException("Exception: $this", cause = this)
}


@ExperimentalFluxoApi
internal operator fun StateFlow<Int>.plus(other: StateFlow<Int>): StateFlow<Int> = CombinedFlowCounter(this, other)

@ExperimentalFluxoApi
private class CombinedFlowCounter(
    private val a: StateFlow<Int>,
    private val b: StateFlow<Int>,
) : StateFlow<Int> {

    private val flow = a.combine(b) { a, b -> a + b }

    override val value: Int get() = a.value + b.value

    /** @see kotlinx.coroutines.flow.StateFlowImpl.replayCache */
    override val replayCache: List<Int> get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<Int>): Nothing {
        @Suppress("CAST_NEVER_SUCCEEDS")
        flow.collect(collector) as Nothing
    }
}


internal class SubscriptionCountFlow<T>(
    private val subscriptionCount: MutableStateFlow<Int>,
    private val delegate: Flow<T>,
) : Flow<T> {

    override suspend fun collect(collector: FlowCollector<T>) {
        try {
            subscriptionCount.getAndAdd(delta = 1)
            delegate.collect(collector)
        } finally {
            subscriptionCount.getAndAdd(delta = -1)
        }
    }
}


/**
 * Atomically adds the given [delta] to the current [value][StateFlow.value].
 *
 * @param delta the value to add
 * @return the previous value
 *
 * @see MutableStateFlow.compareAndSet
 * @see java.util.concurrent.atomic.AtomicInteger.getAndAdd
 */
internal fun MutableStateFlow<Int>.getAndAdd(delta: Int): Int {
    while (true) {
        val value = value
        if (compareAndSet(expect = value, update = value + delta)) {
            return value
        }
    }
}
