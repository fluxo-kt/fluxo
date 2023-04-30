package kt.fluxo.test.compare.naive

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.getAndAdd
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent

internal object NaiveBenchmark {
    fun stateFlowStaticIncrement(): Int {
        val state = MutableStateFlow(0)
        runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(Unit) { state.getAndAdd(1) }
            state.consumeCommonBenchmark(launchDef)
        }
        return state.value
    }
}
