@file:Suppress("KDocUnresolvedReference")

package kt.fluxo.core.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kt.fluxo.common.annotation.ExperimentalFluxoApi

internal fun Throwable?.toCancellationException() = when (this) {
    null -> null
    is CancellationException -> this
    else -> CancellationException("Exception: $this", cause = this)
}


/**
 * Cancel the [Job] on [another job][job] [completion][Job.invokeOnCompletion].
 *
 * @receiver [Job] that cancels after completion of [job]
 * @param job that triggers cancellation
 *
 * @see Job.invokeOnCompletion
 */
internal fun Job.dependOn(job: Job?): DisposableHandle? {
    job ?: return null

    // Fail-fast
    if (!job.isActive) {
        @OptIn(InternalCoroutinesApi::class)
        throw job.getCancellationException()
    }

    // Cancel the job on another Job cancellation
    return job.invokeOnCompletion {
        val current = this
        if (current.isActive) {
            current.cancel(it.toCancellationException())
        }
    }
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
 * @return the earlier value
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
