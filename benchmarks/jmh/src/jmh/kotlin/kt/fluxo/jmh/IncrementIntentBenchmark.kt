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
 * No dispatching, only maximum direct performance possible with state-container.
 * Each operation creates a state store, sends 5000 intents with reduction, and checks state updates.
 * Thread count, forks and iterations come from the jmh_* Gradle properties (see benchmarks/jmh/build.gradle.kts);
 * current results are in the Benchmark workflow summary.
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
