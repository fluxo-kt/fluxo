package kt.fluxo.tests

import kotlinx.coroutines.test.runTest
import kt.fluxo.test.BENCHMARK_REPETITIONS
import kt.fluxo.test.ballast.BallastBenchmark
import kt.fluxo.test.fluxo.FluxoBenchmark
import kt.fluxo.test.mvicore.MviCoreBenchmark
import kt.fluxo.test.mvikotlin.MviKotlinBenchmark
import kt.fluxo.test.naive.NaiveBenchmark
import kt.fluxo.test.orbit.OrbitBenchmark
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("FunctionNaming")
class IncrementIntentsTest {

    @Test
    fun fluxo__mvvm_intent() = test(FluxoBenchmark.mvvmIntent())

    @Test
    fun fluxo__mvi_reducer() = test(FluxoBenchmark.mviReducer())

    @Test
    fun fluxo__mvi_handler() = test(FluxoBenchmark.mviHandler())

    // endregion


    @Test
    fun mvicore__mvi_reducer() = test(MviCoreBenchmark.mviReducer())

    @Test
    fun mvikotlin__mvi_reducer() = test(MviKotlinBenchmark.mviReducer())

    @Test
    fun ballast__mvi_handler() = test(BallastBenchmark.mviHandler())

    @Test
    fun orbit__mvvm_intent() = test(OrbitBenchmark.mvvmIntent())


    @Test
    fun naive__state_flow() = test(NaiveBenchmark.stateFlow())


    private fun test(result: Int) = runTest(dispatchTimeoutMs = 1_000) {
        assertEquals(BENCHMARK_REPETITIONS, result)
    }
}
