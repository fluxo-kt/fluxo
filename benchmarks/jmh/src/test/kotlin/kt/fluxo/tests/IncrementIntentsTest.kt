package kt.fluxo.tests

import kotlinx.coroutines.test.runTest
import kt.fluxo.test.compare.BENCHMARK_REPETITIONS
import kt.fluxo.test.compare.ballast.BallastBenchmark
import kt.fluxo.test.compare.elmslie.ElmslieBenchmark
import kt.fluxo.test.compare.flowredux.FlowReduxBenchmark
import kt.fluxo.test.compare.fluxo.FluxoBenchmark
import kt.fluxo.test.compare.genakureduce.GenakuReduceBenchmark
import kt.fluxo.test.compare.mobiuskt.MobiusKtBenchmark
import kt.fluxo.test.compare.motorrocsm.MotorroCommonStateMachineBenchmark
import kt.fluxo.test.compare.mvicore.MviCoreBenchmark
import kt.fluxo.test.compare.mvikotlin.MviKotlinBenchmark
import kt.fluxo.test.compare.naive.NaiveBenchmark
import kt.fluxo.test.compare.orbit.OrbitBenchmark
import kt.fluxo.test.compare.reduktor.ReduktorBenchmark
import kt.fluxo.test.compare.reduxkotlin.ReduxKotlinBenchmark
import kt.fluxo.test.compare.respawnflowmvi.RespawnFlowMviBenchmark
import kt.fluxo.test.compare.tindersm.TinderStateMachineBenchmark
import kt.fluxo.test.compare.visualfsm.VisualFsmBenchmark
import kotlin.test.Test
import kotlin.test.assertEquals


@Suppress("FunctionNaming")
class IncrementIntentsTest {

    // region Fluxo

    @Test
    fun fluxo__mvvmp_intent() = test(FluxoBenchmark.mvvmpIntentStaticIncrement())

    @Test
    fun fluxo__mvvmp_intent__external_arg() = test(FluxoBenchmark.mvvmpIntentAdd())

    @Test
    fun fluxo__mvi_reducer() = test(FluxoBenchmark.mviReducerStaticIncrement())

    @Test
    fun fluxo__mvi_reducer__external_arg() = test(FluxoBenchmark.mviReducerAdd())

    @Test
    fun fluxo__mvi_handler() = test(FluxoBenchmark.mviHandlerStaticIncrement())

    @Test
    fun fluxo__mvi_handler__external_arg() = test(FluxoBenchmark.mviHandlerAdd())

    // endregion


    @Test
    fun reduxkotlin__mvi_reducer() = test(ReduxKotlinBenchmark.mviReducerStaticIncrement())

    @Test
    fun reduxkotlin__mvi_reducer__external_arg() = test(ReduxKotlinBenchmark.mviReducerAdd())


    @Test
    fun mvicore__mvi_reducer() = test(MviCoreBenchmark.mviReducerStaticIncrement())

    @Test
    fun mvicore__mvi_reducer__external_arg() = test(MviCoreBenchmark.mviReducerAdd())


    @Test
    fun mvikotlin__mvi_reducer() = test(MviKotlinBenchmark.mviReducerStaticIncrement())

    @Test
    fun mvikotlin__mvi_reducer__external_arg() = test(MviKotlinBenchmark.mviReducerAdd())


    @Test
    fun reduktor__mvi_reducer() = test(ReduktorBenchmark.mviReducerStaticIncrement())

    @Test
    fun reduktor__mvi_reducer__external_arg() = test(ReduktorBenchmark.mviReducerAdd())


    @Test
    fun elmslie__elm_reducer() = test(ElmslieBenchmark.elmReducerStaticIncrement())

    @Test
    fun elmslie__elm_reducer__external_arg() = test(ElmslieBenchmark.elmReducerAdd())


    @Test
    fun visualfsm__sm_reducer() = test(VisualFsmBenchmark.smReducerStaticIncrement())

    @Test
    fun visualfsm__sm_reducer__external_arg() = test(VisualFsmBenchmark.smReducerAdd())


    @Test
    fun tindersm__sm_reducer() = test(TinderStateMachineBenchmark.smReducerStaticIncrement())

    @Test
    fun tindersm__sm_reducer__external_arg() = test(TinderStateMachineBenchmark.smReducerAdd())


    @Test
    fun mobiuskt__sm_reducer() = test(MobiusKtBenchmark.smReducerStaticIncrement())

    @Test
    fun mobiuskt__sm_reducer__external_arg() = test(MobiusKtBenchmark.smReducerAdd())


    @Test
    fun ballast__mvi_handler() = test(BallastBenchmark.mviHandlerStaticIncrement())

    @Test
    fun ballast__mvi_handler__external_arg() = test(BallastBenchmark.mviHandlerAdd())


    @Test
    fun flowredux__mvi_handler() = test(FlowReduxBenchmark.mviHandlerStaticIncrement())

    @Test
    fun flowredux__mvi_handler__external_arg() = test(FlowReduxBenchmark.mviHandlerAdd())


    @Test
    fun flowmvi__mvi_handler() = test(RespawnFlowMviBenchmark.mviHandlerStaticIncrement())

    @Test
    fun flowmvi__mvi_handler__external_arg() = test(RespawnFlowMviBenchmark.mviHandlerAdd())


    @Test
    fun genakureduce__mvi_handler() = test(GenakuReduceBenchmark.mviHandlerStaticIncrement())

    @Test
    fun genakureduce__mvi_handler__external_arg() = test(GenakuReduceBenchmark.mviHandlerAdd())


    @Test
    fun orbit__mvvmp_intent() = test(OrbitBenchmark.mvvmpIntentStaticIncrement())

    @Test
    fun orbit__mvvmp_intent__external_arg() = test(OrbitBenchmark.mvvmpIntentAdd())


    @Test
    fun motorrocsm__sm_reducer() = test(MotorroCommonStateMachineBenchmark.smReducerStaticIncrement())

    @Test
    fun motorrocsm__sm_reducer__external_arg() = test(MotorroCommonStateMachineBenchmark.smReducerAdd())


    @Test
    fun naive__state_flow() = test(NaiveBenchmark.stateFlowStaticIncrement())


    private fun test(result: Int) = runTest {
        assertEquals(BENCHMARK_REPETITIONS, result)
    }
}
