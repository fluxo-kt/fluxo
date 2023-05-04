package kt.fluxo.test.compare.motorrocsm

import com.motorro.commonstatemachine.CommonMachineState
import com.motorro.commonstatemachine.coroutines.FlowStateMachine
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent

internal object MotorroCommonStateMachineBenchmark {

    fun smReducerStaticIncrement(): Int {
        lateinit var sm: FlowStateMachine<IntentIncrement, Int>
        val machineState = object : CommonMachineState<IntentIncrement, Int>() {
            override fun doProcess(gesture: IntentIncrement) {
                @Suppress("UNINITIALIZED_VARIABLE")
                when (gesture) {
                    IntentIncrement.Increment -> setUiState(sm.uiState.value + 1)
                }
            }
        }
        sm = FlowStateMachine(initialUiState = 0) { machineState }
        return runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { sm.process(it) }
            val state = sm.uiState.consumeCommonBenchmark(launchDef)
            sm.clear()
            state
        }
    }

    fun smReducerAdd(value: Int = BENCHMARK_ARG): Int {
        lateinit var sm: FlowStateMachine<IntentAdd, Int>
        val machineState = object : CommonMachineState<IntentAdd, Int>() {
            override fun doProcess(gesture: IntentAdd) {
                @Suppress("UNINITIALIZED_VARIABLE")
                when (gesture) {
                    is IntentAdd.Add -> setUiState(sm.uiState.value + gesture.value)
                }
            }
        }
        sm = FlowStateMachine(initialUiState = 0) { machineState }
        return runBlocking {
            val launchDef = launchCommonBenchmark { sm.process(IntentAdd.Add(value = value)) }
            val state = sm.uiState.consumeCommonBenchmark(launchDef)
            sm.clear()
            state
        }
    }
}
