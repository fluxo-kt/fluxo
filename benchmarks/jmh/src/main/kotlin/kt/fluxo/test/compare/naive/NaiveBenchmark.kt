package kt.fluxo.test.compare.naive

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.CommonBenchmark.consumeCommon
import kt.fluxo.test.compare.CommonBenchmark.launchCommon

internal object NaiveBenchmark {
    fun stateFlow(): Int {
        val state = MutableStateFlow(0)
        runBlocking {
            val launchDef = launchCommon(Unit) { state.getAndAdd(1) }
            consumeCommon(state, launchDef)
        }
        return state.value
    }

    private fun MutableStateFlow<Int>.getAndAdd(delta: Int): Int {
        while (true) {
            val value = value
            if (compareAndSet(value, value + delta)) {
                return value
            }
        }
    }
}
