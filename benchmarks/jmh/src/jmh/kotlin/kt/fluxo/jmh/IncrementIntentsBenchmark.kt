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
 * Benchmark                                          Mode  Cnt   Score    Error   Units
 * IncrementIntentsBenchmark.naive__state_flow       thrpt   18  16.712 ±  0.129  ops/ms
 * IncrementIntentsBenchmark.mvicore__mvi_reducer    thrpt   18   3.055 ±  0.034  ops/ms
 * IncrementIntentsBenchmark.mvikotlin__mvi_reducer  thrpt   18   1.940 ±  0.153  ops/ms
 * IncrementIntentsBenchmark.ballast__mvi_handler    thrpt   18   0.383 ±  0.014  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvvm_intent      thrpt   18   0.270 ±  0.002  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvi_handler      thrpt   18   0.264 ±  0.005  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvi_reducer      thrpt   18   0.263 ±  0.005  ops/ms
 * IncrementIntentsBenchmark.orbit__mvvm_intent      thrpt   18   0.203 ±  0.004  ops/ms
 *
 * IncrementIntentsBenchmark.naive__state_flow        avgt   18   0.060 ±  0.001   ms/op
 * IncrementIntentsBenchmark.mvicore__mvi_reducer     avgt   18   0.322 ±  0.005   ms/op
 * IncrementIntentsBenchmark.mvikotlin__mvi_reducer   avgt   18   0.534 ±  0.039   ms/op
 * IncrementIntentsBenchmark.ballast__mvi_handler     avgt   18   2.507 ±  0.012   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_reducer       avgt   18   3.505 ±  0.274   ms/op
 * IncrementIntentsBenchmark.fluxo__mvvm_intent       avgt   18   3.716 ±  0.049   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_handler       avgt   18   3.751 ±  0.038   ms/op
 * IncrementIntentsBenchmark.orbit__mvvm_intent       avgt   18   5.071 ±  0.115   ms/op
 *
 * IncrementIntentsBenchmark.naive__state_flow          ss   18   0.549 ±  0.083   ms/op
 * IncrementIntentsBenchmark.mvicore__mvi_reducer       ss   18   3.573 ±  1.114   ms/op
 * IncrementIntentsBenchmark.mvikotlin__mvi_reducer     ss   18   3.925 ±  1.105   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_handler         ss   18  14.823 ±  2.669   ms/op
 * IncrementIntentsBenchmark.fluxo__mvvm_intent         ss   18  15.108 ±  2.798   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_reducer         ss   18  15.700 ±  1.419   ms/op
 * IncrementIntentsBenchmark.ballast__mvi_handler       ss   18  16.108 ±  2.178   ms/op
 * IncrementIntentsBenchmark.orbit__mvvm_intent         ss   18  19.027 ±  3.477   ms/op
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
