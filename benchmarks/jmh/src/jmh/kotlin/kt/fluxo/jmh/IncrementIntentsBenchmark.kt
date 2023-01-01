package kt.fluxo.jmh

import kt.fluxo.test.ballast.BallastBenchmark
import kt.fluxo.test.fluxo.FluxoBenchmark
import kt.fluxo.test.mvicore.MviCoreBenchmark
import kt.fluxo.test.mvikotlin.MviKotlinBenchmark
import kt.fluxo.test.naive.NaiveBenchmark
import kt.fluxo.test.orbit.OrbitBenchmark
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

/**
 * Benchmark for simple incrementing intent throughput.
 *
 * _Example results:_
 * ```
 * Benchmark                                          Mode  Cnt    Score    Error   Units
 * IncrementIntentsBenchmark.naive__state_flow       thrpt    6   16.543 ±  0.459  ops/ms
 * IncrementIntentsBenchmark.mvicore__mvi_reducer    thrpt    6    3.050 ±  0.033  ops/ms
 * IncrementIntentsBenchmark.mvikotlin__mvi_reducer  thrpt    6    1.805 ±  0.191  ops/ms
 * IncrementIntentsBenchmark.ballast__mvi_handler    thrpt    6    0.372 ±  0.109  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvi_handler      thrpt    6    0.241 ±  0.056  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvi_reducer      thrpt    6    0.218 ±  0.022  ops/ms
 * IncrementIntentsBenchmark.orbit__mvvm_intent      thrpt    6    0.193 ±  0.013  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvvm_intent      thrpt    6    0.017 ±  0.001  ops/ms
 *
 * IncrementIntentsBenchmark.naive__state_flow        avgt    6    0.071 ±  0.009   ms/op
 * IncrementIntentsBenchmark.mvicore__mvi_reducer     avgt    6    0.377 ±  0.149   ms/op
 * IncrementIntentsBenchmark.mvikotlin__mvi_reducer   avgt    6    0.527 ±  0.062   ms/op
 * IncrementIntentsBenchmark.ballast__mvi_handler     avgt    6    2.538 ±  0.038   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_handler       avgt    6    3.622 ±  0.206   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_reducer       avgt    6    4.362 ±  0.162   ms/op
 * IncrementIntentsBenchmark.orbit__mvvm_intent       avgt    6    6.682 ±  1.436   ms/op
 * IncrementIntentsBenchmark.fluxo__mvvm_intent       avgt    6   58.633 ±  1.737   ms/op
 *
 * IncrementIntentsBenchmark.naive__state_flow          ss    6    0.642 ±  0.087   ms/op
 * IncrementIntentsBenchmark.mvicore__mvi_reducer       ss    6    4.348 ±  2.664   ms/op
 * IncrementIntentsBenchmark.mvikotlin__mvi_reducer     ss    6    5.327 ±  4.415   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_reducer         ss    6   26.751 ± 15.966   ms/op
 * IncrementIntentsBenchmark.ballast__mvi_handler       ss    6   30.239 ± 16.781   ms/op
 * IncrementIntentsBenchmark.orbit__mvvm_intent         ss    6   36.130 ± 21.458   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_handler         ss    6   38.908 ± 51.693   ms/op
 * IncrementIntentsBenchmark.fluxo__mvvm_intent         ss    6  135.051 ± 34.694   ms/op
 * ```
 */
@State(Scope.Benchmark)
@Suppress("FunctionNaming", "FunctionName")
open class IncrementIntentsBenchmark {
    // region Fluxo

    @Benchmark
    fun fluxo__mvvm_intent(bh: Blackhole) = bh.consume(FluxoBenchmark.mvvmIntent())

    @Benchmark
    fun fluxo__mvi_reducer(bh: Blackhole) = bh.consume(FluxoBenchmark.mviReducer())

    @Benchmark
    fun fluxo__mvi_handler(bh: Blackhole) = bh.consume(FluxoBenchmark.mviHandler())

    // endregion


    @Benchmark
    fun mvicore__mvi_reducer(bh: Blackhole) = bh.consume(MviCoreBenchmark.mviReducer())

    @Benchmark
    fun mvikotlin__mvi_reducer(bh: Blackhole) = bh.consume(MviKotlinBenchmark.mviReducer())

    @Benchmark
    fun ballast__mvi_handler(bh: Blackhole) = bh.consume(BallastBenchmark.mviHandler())

    @Benchmark
    fun orbit__mvvm_intent(bh: Blackhole) = bh.consume(OrbitBenchmark.mvvmIntent())


    @Benchmark
    fun naive__state_flow(bh: Blackhole) = bh.consume(NaiveBenchmark.stateFlow())
}
