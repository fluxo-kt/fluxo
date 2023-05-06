package kt.fluxo.jmh

import kt.fluxo.test.compare.ballast.BallastBenchmark
import kt.fluxo.test.compare.flowredux.FlowReduxBenchmark
import kt.fluxo.test.compare.fluxo.FluxoBenchmark
import kt.fluxo.test.compare.genakureduce.GenakuReduceBenchmark
import kt.fluxo.test.compare.mvicore.MviCoreBenchmark
import kt.fluxo.test.compare.mvikotlin.MviKotlinBenchmark
import kt.fluxo.test.compare.naive.NaiveBenchmark
import kt.fluxo.test.compare.orbit.OrbitBenchmark
import kt.fluxo.test.compare.reduktor.ReduktorBenchmark
import kt.fluxo.test.compare.reduxkotlin.ReduxKotlinBenchmark
import kt.fluxo.test.compare.respawnflowmvi.RespawnFlowMviBenchmark
import kt.fluxo.test.compare.visualfsm.VisualFsmBenchmark
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

/**
 * Benchmark for simple incrementing intent throughput.
 *
 * Don't try comparing dispatching here,
 * only maximum direct performance possible with state-container.
 *
 * _Example results:_
 * ```
 * Benchmark                Mode  Cnt   Score    Error   Units  Percent
 * naive__state_flow       thrpt   18   9.533 ±  0.322  ops/ms    86.7%
 * mvicore__mvi_reducer    thrpt   18   5.107 ±  0.165  ops/ms     0.0%
 * fluxo__mvi_reducer      thrpt   18   3.772 ±  0.247  ops/ms   -26.1%
 * mvikotlin__mvi_reducer  thrpt   18   1.781 ±  0.063  ops/ms   -65.1%
 * fluxo__mvvmp_intent     thrpt   18   1.276 ±  0.164  ops/ms   -75.0%
 * fluxo__mvi_handler      thrpt   18   1.271 ±  0.100  ops/ms   -75.1%
 * ballast__mvi_handler    thrpt   18   0.389 ±  0.116  ops/ms   -92.4%
 * orbit__mvvmp_intent     thrpt   18   0.259 ±  0.007  ops/ms   -94.9%
 *
 * naive__state_flow        avgt   18   0.390 ±  0.026   ms/op   -51.6%
 * mvicore__mvi_reducer     avgt   18   0.805 ±  0.046   ms/op     0.0%
 * fluxo__mvi_reducer       avgt   18   0.939 ±  0.041   ms/op    16.6%
 * mvikotlin__mvi_reducer   avgt   18   2.201 ±  0.066   ms/op   173.4%
 * fluxo__mvvmp_intent      avgt   18   3.222 ±  0.117   ms/op   300.2%
 * fluxo__mvi_handler       avgt   18   3.329 ±  0.192   ms/op   313.5%
 * ballast__mvi_handler     avgt   18   7.912 ±  0.365   ms/op   882.9%
 * orbit__mvvmp_intent      avgt   18  14.025 ±  0.478   ms/op  1642.2%
 * ```
 */
@State(Scope.Benchmark)
@Suppress("FunctionNaming", "FunctionName", "TooManyFunctions")
open class IncrementIntentBenchmark {
    // region Fluxo

    @Benchmark
    fun fluxo__mvvmp_intent(bh: Blackhole) = bh.consume(FluxoBenchmark.mvvmpIntentAdd())

    @Benchmark
    fun fluxo__mvi_reducer(bh: Blackhole) = bh.consume(FluxoBenchmark.mviReducerAdd())

    @Benchmark
    fun fluxo__mvi_handler(bh: Blackhole) = bh.consume(FluxoBenchmark.mviHandlerAdd())

    // endregion


    @Benchmark
    fun reduxkotlin__mvi_reducer(bh: Blackhole) = bh.consume(ReduxKotlinBenchmark.mviReducerAdd())

    @Benchmark
    fun mvicore__mvi_reducer(bh: Blackhole) = bh.consume(MviCoreBenchmark.mviReducerAdd())

    @Benchmark
    fun mvikotlin__mvi_reducer(bh: Blackhole) = bh.consume(MviKotlinBenchmark.mviReducerAdd())

    @Benchmark
    fun reduktor__mvi_reducer(bh: Blackhole) = bh.consume(ReduktorBenchmark.mviReducerAdd())

    @Benchmark
    fun visualfsm__sm_reducer(bh: Blackhole) = bh.consume(VisualFsmBenchmark.smReducerAdd())

    @Benchmark
    fun ballast__mvi_handler(bh: Blackhole) = bh.consume(BallastBenchmark.mviHandlerAdd())

    @Benchmark
    fun flowredux__mvi_handler(bh: Blackhole) = bh.consume(FlowReduxBenchmark.mviHandlerAdd())

    @Benchmark
    fun flowmvi__mvi_handler(bh: Blackhole) = bh.consume(RespawnFlowMviBenchmark.mviHandlerAdd())

    @Benchmark
    fun genakureduce__mvi_handler(bh: Blackhole) = bh.consume(GenakuReduceBenchmark.mviHandlerAdd())

    @Benchmark
    fun orbit__mvvmp_intent(bh: Blackhole) = bh.consume(OrbitBenchmark.mvvmpIntentAdd())


    @Benchmark
    fun naive__state_flow(bh: Blackhole) = bh.consume(NaiveBenchmark.stateFlowStaticIncrement())
}
