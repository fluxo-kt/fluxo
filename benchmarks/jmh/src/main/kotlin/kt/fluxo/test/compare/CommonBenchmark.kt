package kt.fluxo.test.compare

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kt.fluxo.core.internal.Closeable

internal inline fun <I> CoroutineScope.launchCommonBenchmarkWithStaticIntent(
    intent: I,
    yield: Boolean = false,
    crossinline send: suspend (I) -> Unit,
) = launch {
    repeat(BENCHMARK_REPETITIONS) {
        send(intent)
        // Yield makes everything super slow!
        if (yield) yield()
    }
}

internal inline fun CoroutineScope.launchCommonBenchmark(
    crossinline send: suspend () -> Unit,
) = launch {
    repeat(BENCHMARK_REPETITIONS) {
        send()
    }
}

internal suspend fun <T> Flow<T>.consumeCommonBenchmark(
    launchDef: Job,
    parentJob: Job? = null,
    closeable: Closeable? = null,
    transform: (T) -> Int,
): Int {
    val state = transform(first { transform(it) >= BENCHMARK_REPETITIONS })

    launchDef.join()
    parentJob?.cancelAndJoin()
    @Suppress("BlockingMethodInNonBlockingContext")
    closeable?.close()
    return state
}

internal suspend fun Flow<Int>.consumeCommonBenchmark(launchDef: Job, parentJob: Job? = null, closeable: Closeable? = null): Int {
    val state = first { it >= BENCHMARK_REPETITIONS }

    launchDef.join()
    parentJob?.cancelAndJoin()
    @Suppress("BlockingMethodInNonBlockingContext")
    closeable?.close()
    return state
}

internal fun MutableStateFlow<Int>.getAndAdd(delta: Int): Int {
    while (true) {
        val value = value
        if (compareAndSet(value, value + delta)) {
            return value
        }
    }
}

internal fun MutableStateFlow<Int>.addAndGet(delta: Int): Int = getAndAdd(delta) + delta
