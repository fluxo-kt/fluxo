@file:Suppress("FunctionNaming", "FunctionName", "TrailingCommaOnCallSite")

package kt.fluxo.jmh

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kt.fluxo.core.Store
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.intent
import kt.fluxo.core.store
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

/**
 * Benchmark for simple incrementing intent throughput.
 *
 * _Example results:_
 * ```
 * Benchmark                                       Mode  Cnt   Score    Error  Units
 * IncrementIntentsBenchmark.fluxo__mvi_handler    thrpt     4   0.136 ±  0.056  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvi_reducer    thrpt     4   0.144 ±  0.050  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvvm_intent    thrpt     4   0.024 ±  0.010  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvi_handler     avgt     4   6.938 ±  3.288   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_reducer     avgt     4   7.349 ±  3.533   ms/op
 * IncrementIntentsBenchmark.fluxo__mvvm_intent     avgt     4  38.872 ±  5.528   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_handler       ss     4  32.425 ± 34.591   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_reducer       ss     4  33.467 ± 34.906   ms/op
 * IncrementIntentsBenchmark.fluxo__mvvm_intent       ss     4  74.827 ± 45.706   ms/op
 * ```
 */
@State(Scope.Benchmark)
open class IncrementIntentsBenchmark {
    private companion object {
        private const val REPETITIONS = 3_000
    }


    @Benchmark
    fun fluxo__mvvm_intent(bh: Blackhole) {
        val container = container(0)
        val intent: suspend StoreScope<Nothing, Int, Nothing>.() -> Unit = { updateState { it + 1 } }
        repeat(REPETITIONS) { container.intent(intent) }
        consumeFluxo(container, bh)
    }

    @Benchmark
    fun fluxo__mvi_reducer(bh: Blackhole) {
        val container = store<Intent, Int>(0, reducer = {
            when (it) {
                Intent.Increment -> this + 1
            }
        })
        repeat(REPETITIONS) { container.send(Intent.Increment) }
        consumeFluxo(container, bh)
    }

    @Benchmark
    fun fluxo__mvi_handler(bh: Blackhole) {
        val container = store<Intent, Int>(0, handler = { intent ->
            when (intent) {
                Intent.Increment -> updateState { it + 1 }
            }
        })
        repeat(REPETITIONS) { container.send(Intent.Increment) }
        consumeFluxo(container, bh)
    }

    private enum class Intent {
        Increment,
    }

    private fun consumeFluxo(container: Store<*, Int, *>, bh: Blackhole) {
        runBlocking {
            container.stateFlow.first { it == REPETITIONS }

            @OptIn(ExperimentalFluxoApi::class)
            container.closeAndWait()
        }
        bh.consume(container.state)
    }
}
