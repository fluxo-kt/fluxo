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
 * Benchmark                Mode  Cnt   Score   Error   Units
 * naive__state_flow       thrpt   24  17.524 ± 0.196  ops/ms
 * mvicore__mvi_reducer    thrpt   24   3.221 ± 0.035  ops/ms
 * mvikotlin__mvi_reducer  thrpt   24   1.923 ± 0.051  ops/ms
 * ballast__mvi_handler    thrpt   24   0.391 ± 0.003  ops/ms
 * fluxo__mvvm_intent      thrpt   24   0.283 ± 0.003  ops/ms
 * fluxo__mvi_handler      thrpt   24   0.276 ± 0.005  ops/ms
 * fluxo__mvi_reducer      thrpt   24   0.270 ± 0.005  ops/ms
 * orbit__mvvm_intent      thrpt   24   0.190 ± 0.002  ops/ms
 *
 * naive__state_flow        avgt   24   0.060 ± 0.002   ms/op
 * mvicore__mvi_reducer     avgt   24   0.303 ± 0.005   ms/op
 * mvikotlin__mvi_reducer   avgt   24   0.526 ± 0.017   ms/op
 * ballast__mvi_handler     avgt   24   2.672 ± 0.085   ms/op
 * fluxo__mvi_handler       avgt   24   3.683 ± 0.042   ms/op
 * fluxo__mvvm_intent       avgt   24   3.746 ± 0.081   ms/op
 * fluxo__mvi_reducer       avgt   24   3.751 ± 0.054   ms/op
 * orbit__mvvm_intent       avgt   24   5.037 ± 0.095   ms/op
 *
 * naive__state_flow          ss   24   0.675 ± 0.168   ms/op
 * mvicore__mvi_reducer       ss   24   3.398 ± 0.688   ms/op
 * mvikotlin__mvi_reducer     ss   24   4.358 ± 1.297   ms/op
 * ballast__mvi_handler       ss   24  14.208 ± 1.191   ms/op
 * fluxo__mvi_handler         ss   24  14.824 ± 2.344   ms/op
 * fluxo__mvi_reducer         ss   24  15.361 ± 1.911   ms/op
 * fluxo__mvvm_intent         ss   24  16.988 ± 2.267   ms/op
 * orbit__mvvm_intent         ss   24  19.059 ± 2.714   ms/op
 * ```
 */
@State(Scope.Benchmark)
@Suppress("FunctionNaming", "FunctionName")
open class IncrementIntentBenchmark {
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
