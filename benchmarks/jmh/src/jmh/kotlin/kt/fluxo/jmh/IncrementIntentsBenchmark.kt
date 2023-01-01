package kt.fluxo.jmh

import kt.fluxo.test.ballast.BallastBenchmark
import kt.fluxo.test.fluxo.FluxoBenchmark
import kt.fluxo.test.mvicore.MviCoreBenchmark
import kt.fluxo.test.mvikotlin.MviKotlinBenchmark
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
 * IncrementIntentsBenchmark.mvicore__mvi_reducer    thrpt    6    2.399 ±  0.652  ops/ms
 * IncrementIntentsBenchmark.mvikotlin__mvi_reducer  thrpt    6    0.428 ±  0.009  ops/ms
 * IncrementIntentsBenchmark.ballast__mvi_handler    thrpt    6    0.417 ±  0.038  ops/ms
 * IncrementIntentsBenchmark.orbit__mvvm_intent      thrpt    6    0.203 ±  0.006  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvi_handler      thrpt    6    0.179 ±  0.018  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvi_reducer      thrpt    6    0.175 ±  0.011  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvvm_intent      thrpt    6    0.018 ±  0.003  ops/ms
 *
 * IncrementIntentsBenchmark.mvicore__mvi_reducer     avgt    6    0.377 ±  0.032   ms/op
 * IncrementIntentsBenchmark.mvikotlin__mvi_reducer   avgt    6    2.409 ±  0.127   ms/op
 * IncrementIntentsBenchmark.ballast__mvi_handler     avgt    6    2.517 ±  0.247   ms/op
 * IncrementIntentsBenchmark.orbit__mvvm_intent       avgt    6    5.436 ±  0.501   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_reducer       avgt    6    5.561 ±  0.788   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_handler       avgt    6    5.768 ±  1.268   ms/op
 * IncrementIntentsBenchmark.fluxo__mvvm_intent       avgt    6   61.107 ± 22.624   ms/op
 *
 * IncrementIntentsBenchmark.mvicore__mvi_reducer       ss    6    8.944 ± 14.073   ms/op
 * IncrementIntentsBenchmark.ballast__mvi_handler       ss    6   20.474 ± 13.833   ms/op
 * IncrementIntentsBenchmark.mvikotlin__mvi_reducer     ss    6   21.143 ± 11.729   ms/op
 * IncrementIntentsBenchmark.orbit__mvvm_intent         ss    6   25.228 ±  3.836   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_handler         ss    6   30.315 ± 14.676   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_reducer         ss    6   37.513 ± 36.894   ms/op
 * IncrementIntentsBenchmark.fluxo__mvvm_intent         ss    6  122.150 ± 31.872   ms/op
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
}
