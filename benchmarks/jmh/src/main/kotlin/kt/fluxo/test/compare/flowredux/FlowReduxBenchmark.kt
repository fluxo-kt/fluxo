package kt.fluxo.test.compare.flowredux

import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import com.freeletics.flowredux.dsl.InStateBuilderBlock
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent

internal object FlowReduxBenchmark {

    private fun <I : Any> createStateMachine(block: InStateBuilderBlock<Int, Int, I>.() -> Unit): FlowReduxStateMachine<Int, I> {
        return object : FlowReduxStateMachine<Int, I>(initialState = 0) {
            init {
                spec { inState<Int>(block) }
            }
        }
    }

    fun mviHandlerStaticIncrement(): Int {
        val sm = createStateMachine<IntentIncrement> {
            on<IntentIncrement> { _, state ->
                state.override { this + 1 }
            }
        }
        return runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { sm.dispatch(it) }
            sm.state.consumeCommonBenchmark(launchDef)
        }
    }

    fun mviHandlerAdd(value: Int = BENCHMARK_ARG): Int {
        val sm = createStateMachine<IntentAdd> {
            on<IntentAdd.Add> { intent, state ->
                state.override { this + intent.value }
            }
        }
        return runBlocking {
            val launchDef = launchCommonBenchmark { sm.dispatch(IntentAdd.Add(value = value)) }
            sm.state.consumeCommonBenchmark(launchDef)
        }
    }
}
