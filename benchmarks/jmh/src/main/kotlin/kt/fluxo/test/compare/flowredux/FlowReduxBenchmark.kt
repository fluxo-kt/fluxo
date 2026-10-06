package kt.fluxo.test.compare.flowredux

import com.freeletics.flowredux2.FlowReduxStateMachine
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.InStateBuilder
import com.freeletics.flowredux2.initializeWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent

internal object FlowReduxBenchmark {

    // FlowRedux 2 runs a machine in the scope it is launched in; `job` is cancelled when the benchmark run ends.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <I : Any> CoroutineScope.launchStateMachine(
        job: Job,
        block: InStateBuilder<Int, Int, I>.() -> Unit,
    ): FlowReduxStateMachine<StateFlow<Int>, I> {
        val factory = object : FlowReduxStateMachineFactory<Int, I>() {
            init {
                initializeWith { 0 }
                spec { inState<Int>(block) }
            }
        }
        return factory.launchIn(this + job)
    }

    fun mviHandlerStaticIncrement(): Int = runBlocking {
        val job = Job()
        val sm = launchStateMachine<IntentIncrement>(job) {
            on<IntentIncrement> { override { this + 1 } }
        }
        val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { sm.dispatch(it) }
        sm.state.consumeCommonBenchmark(launchDef, parentJob = job)
    }

    fun mviHandlerAdd(value: Int = BENCHMARK_ARG): Int = runBlocking {
        val job = Job()
        val sm = launchStateMachine<IntentAdd>(job) {
            on<IntentAdd.Add> { intent -> override { this + intent.value } }
        }
        val launchDef = launchCommonBenchmark { sm.dispatch(IntentAdd.Add(value = value)) }
        sm.state.consumeCommonBenchmark(launchDef, parentJob = job)
    }
}
