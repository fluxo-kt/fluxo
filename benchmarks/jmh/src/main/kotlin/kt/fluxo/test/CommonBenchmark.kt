package kt.fluxo.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kt.fluxo.core.internal.Closeable

internal object CommonBenchmark {

    internal inline fun <I> CoroutineScope.launchCommon(intent: I, crossinline send: suspend (I) -> Unit) = launch {
        @Suppress("UnnecessaryVariable")
        val i = intent
        repeat(BENCHMARK_REPETITIONS) {
            send(i)
            yield()
        }
    }

    internal suspend fun consumeCommon(
        stateFlow: Flow<Int>,
        launchDef: Job,
        parentJob: Job? = null,
        dispatcher: Closeable? = null,
    ): Int {
        val state = stateFlow.first { it >= BENCHMARK_REPETITIONS }

        launchDef.join()
        parentJob?.cancelAndJoin()
        @Suppress("BlockingMethodInNonBlockingContext")
        dispatcher?.close()
        return state
    }
}
