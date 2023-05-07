package kt.fluxo.jmh

import kt.fluxo.test.compare.ballast.BallastBenchmark
import kt.fluxo.test.compare.elmslie.ElmslieBenchmark
import kt.fluxo.test.compare.flowredux.FlowReduxBenchmark
import kt.fluxo.test.compare.fluxo.FluxoBenchmark
import kt.fluxo.test.compare.genakureduce.GenakuReduceBenchmark
import kt.fluxo.test.compare.mobiuskt.MobiusKtBenchmark
import kt.fluxo.test.compare.mvicore.MviCoreBenchmark
import kt.fluxo.test.compare.mvikotlin.MviKotlinBenchmark
import kt.fluxo.test.compare.naive.NaiveBenchmark
import kt.fluxo.test.compare.orbit.OrbitBenchmark
import kt.fluxo.test.compare.reduktor.ReduktorBenchmark
import kt.fluxo.test.compare.reduxkotlin.ReduxKotlinBenchmark
import kt.fluxo.test.compare.respawnflowmvi.RespawnFlowMviBenchmark
import kt.fluxo.test.compare.tindersm.TinderStateMachineBenchmark
import kt.fluxo.test.compare.visualfsm.VisualFsmBenchmark
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

/**
 * Benchmark for simple incrementing intent throughput.
 *
 * Single-thread.
 * No dispatching, only maximum direct performance possible with state-container.
 *
 * Each operation creates a state store, sends 5000 intents with reduction, and checks state updates.
 *
 * _Example results:_
 * ```
 * Benchmark                   Mode  Cnt   Score   Error   Units  Percent
 * naive__state_flow          thrpt   12  11.235 ± 0.055  ops/ms    12.9%
 * reduxkotlin__mvi_reducer   thrpt   12   9.949 ± 0.105  ops/ms     0.0%
 * mvicore__mvi_reducer       thrpt   12   5.787 ± 0.029  ops/ms   -41.8%
 * visualfsm__sm_reducer      thrpt   12   5.239 ± 0.037  ops/ms   -47.3%
 * fluxo__mvi_reducer         thrpt   12   5.106 ± 0.089  ops/ms   -48.7%
 * mvikotlin__mvi_reducer     thrpt   12   2.074 ± 0.049  ops/ms   -79.2%
 * reduktor__mvi_reducer      thrpt   12   1.869 ± 0.021  ops/ms   -81.2%
 * fluxo__mvvmp_intent        thrpt   12   1.457 ± 0.025  ops/ms   -85.4%
 * genakureduce__mvi_handler  thrpt   12   1.443 ± 0.018  ops/ms   -85.5%
 * fluxo__mvi_handler         thrpt   12   1.438 ± 0.036  ops/ms   -85.5%
 * orbit__mvvmp_intent        thrpt   12   0.639 ± 0.021  ops/ms   -93.6%
 * ballast__mvi_handler       thrpt   12   0.597 ± 0.016  ops/ms   -94.0%
 * flowmvi__mvi_handler       thrpt   12   0.445 ± 0.013  ops/ms   -95.5%
 * flowredux__mvi_handler     thrpt   12   0.112 ± 0.003  ops/ms   -98.9%
 *
 * naive__state_flow           avgt   12   0.260 ± 0.004   ms/op   -15.9%
 * reduxkotlin__mvi_reducer    avgt   12   0.309 ± 0.004   ms/op     0.0%
 * mvicore__mvi_reducer        avgt   12   0.510 ± 0.002   ms/op    65.0%
 * visualfsm__sm_reducer       avgt   12   0.564 ± 0.003   ms/op    82.5%
 * fluxo__mvi_reducer          avgt   12   0.612 ± 0.004   ms/op    98.1%
 * mvikotlin__mvi_reducer      avgt   12   1.449 ± 0.070   ms/op   368.9%
 * reduktor__mvi_reducer       avgt   12   1.593 ± 0.018   ms/op   415.5%
 * fluxo__mvi_handler          avgt   12   1.960 ± 0.363   ms/op   534.3%
 * fluxo__mvvmp_intent         avgt   12   2.051 ± 0.017   ms/op   563.8%
 * genakureduce__mvi_handler   avgt   12   2.052 ± 0.023   ms/op   564.1%
 * orbit__mvvmp_intent         avgt   12   4.590 ± 0.111   ms/op  1385.4%
 * ballast__mvi_handler        avgt   12   5.088 ± 0.346   ms/op  1546.6%
 * flowmvi__mvi_handler        avgt   12   6.611 ± 0.468   ms/op  2039.5%
 * flowredux__mvi_handler      avgt   12  25.454 ± 0.459   ms/op  8137.5%
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
    fun elmslie__elm_reducer(bh: Blackhole) = bh.consume(ElmslieBenchmark.elmReducerAdd())

    @Benchmark
    fun visualfsm__sm_reducer(bh: Blackhole) = bh.consume(VisualFsmBenchmark.smReducerAdd())

    @Benchmark
    fun tindersm__sm_reducer(bh: Blackhole) = bh.consume(TinderStateMachineBenchmark.smReducerAdd())

    @Benchmark
    fun mobiuskt__sm_reducer(bh: Blackhole) = bh.consume(MobiusKtBenchmark.smReducerAdd())

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
