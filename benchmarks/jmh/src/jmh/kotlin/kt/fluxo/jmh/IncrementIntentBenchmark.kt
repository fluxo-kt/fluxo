package kt.fluxo.jmh

import kt.fluxo.test.compare.ballast.BallastBenchmark
import kt.fluxo.test.compare.fluxo.FluxoBenchmark
import kt.fluxo.test.compare.mvicore.MviCoreBenchmark
import kt.fluxo.test.compare.mvikotlin.MviKotlinBenchmark
import kt.fluxo.test.compare.naive.NaiveBenchmark
import kt.fluxo.test.compare.orbit.OrbitBenchmark
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

/**
 * Benchmark for simple incrementing intent throughput.
 *
 * _Example results:_
 * ```
 * Benchmark                Mode  Cnt   Score   Error   Units  Percent
 * naive__state_flow       thrpt  150  42.678 ± 0.316  ops/ms   431.2%
 * mvicore__mvi_reducer    thrpt  150   8.034 ± 0.217  ops/ms     0.0%
 * mvikotlin__mvi_reducer  thrpt  150   4.567 ± 0.565  ops/ms   -43.2%
 * fluxo__mvi_reducer      thrpt  150   0.779 ± 0.017  ops/ms   -90.3%
 * ballast__mvi_handler    thrpt  150   0.695 ± 0.005  ops/ms   -91.3%
 * fluxo__mvvm_intent      thrpt  150   0.673 ± 0.006  ops/ms   -91.6%
 * fluxo__mvi_handler      thrpt  150   0.669 ± 0.010  ops/ms   -91.7%
 * orbit__mvvm_intent      thrpt  150   0.250 ± 0.001  ops/ms   -96.9%
 *
 * naive__state_flow        avgt  150   0.141 ± 0.001   ms/op   -81.0%
 * mvicore__mvi_reducer     avgt  150   0.742 ± 0.022   ms/op     0.0%
 * mvikotlin__mvi_reducer   avgt  150   2.192 ± 0.069   ms/op   195.4%
 * fluxo__mvi_reducer       avgt  150   7.708 ± 0.198   ms/op   938.8%
 * fluxo__mvi_handler       avgt  150   8.774 ± 0.059   ms/op  1082.5%
 * fluxo__mvvm_intent       avgt  150   8.860 ± 0.115   ms/op  1094.1%
 * ballast__mvi_handler     avgt  150   9.092 ± 0.107   ms/op  1125.3%
 * orbit__mvvm_intent       avgt  150  24.336 ± 0.216   ms/op  3179.8%
 *
 * naive__state_flow          ss  150   0.440 ± 0.173   ms/op   -81.3%
 * mvicore__mvi_reducer       ss  150   2.353 ± 0.319   ms/op     0.0%
 * mvikotlin__mvi_reducer     ss  150   2.777 ± 0.145   ms/op    18.0%
 * fluxo__mvi_reducer         ss  150  11.307 ± 0.759   ms/op   380.5%
 * fluxo__mvvm_intent         ss  150  11.351 ± 0.568   ms/op   382.4%
 * fluxo__mvi_handler         ss  150  11.649 ± 0.839   ms/op   395.1%
 * ballast__mvi_handler       ss  150  14.384 ± 1.338   ms/op   511.3%
 * orbit__mvvm_intent         ss  150  35.009 ± 3.054   ms/op  1387.8%
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
