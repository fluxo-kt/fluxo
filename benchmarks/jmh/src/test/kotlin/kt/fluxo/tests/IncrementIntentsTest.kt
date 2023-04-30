package kt.fluxo.tests

import kotlinx.coroutines.test.runTest
import kt.fluxo.test.compare.BENCHMARK_REPETITIONS
import kt.fluxo.test.compare.ballast.BallastBenchmark
import kt.fluxo.test.compare.fluxo.FluxoBenchmark
import kt.fluxo.test.compare.mvicore.MviCoreBenchmark
import kt.fluxo.test.compare.mvikotlin.MviKotlinBenchmark
import kt.fluxo.test.compare.naive.NaiveBenchmark
import kt.fluxo.test.compare.orbit.OrbitBenchmark
import kotlin.test.Test
import kotlin.test.assertEquals


@Suppress("FunctionNaming")
class IncrementIntentsTest {

    @Test
    fun fluxo__mvvm_intent() = test(FluxoBenchmark.mvvmpIntentStaticIncrement())

    @Test
    fun fluxo__mvi_reducer() = test(FluxoBenchmark.mviReducerStaticIncrement())

    @Test
    fun fluxo__mvi_handler() = test(FluxoBenchmark.mviHandlerStaticIncrement())

    // endregion


    @Test
    fun mvicore__mvi_reducer() = test(MviCoreBenchmark.mviReducerStaticIncrement())

    @Test
    fun mvikotlin__mvi_reducer() = test(MviKotlinBenchmark.mviReducerStaticIncrement())

    @Test
    fun ballast__mvi_handler() = test(BallastBenchmark.mviHandlerStaticIncrement())

    @Test
    fun orbit__mvvm_intent() = test(OrbitBenchmark.mvvmpIntentStaticIncrement())


    @Test
    fun naive__state_flow() = test(NaiveBenchmark.stateFlowStaticIncrement())


    private fun test(result: Int) = runTest {
        assertEquals(BENCHMARK_REPETITIONS, result)
    }
}
