package kt.fluxo.test.compare.genakureduce

import com.genaku.reduce.KnotBuilder
import com.genaku.reduce.KnotImpl
import com.genaku.reduce.Reducer
import com.genaku.reduce.State
import com.genaku.reduce.StateIntent
import com.genaku.reduce.knot
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent

internal object GenakuReduceBenchmark {

    @Suppress("InjectDispatcher")
    private fun <I : StateIntent> createStoreAndJob(
        reducer: KnotBuilder<ReduceState, I, Nothing>.() -> Reducer<ReduceState, I, Nothing>,
    ): Pair<KnotImpl<ReduceState, I, Nothing>, CompletableJob> {
        val dispatcher = Dispatchers.Unconfined
        val job = SupervisorJob()
        val scope = CoroutineScope(dispatcher + job)
        val knot = knot {
            initialState = ReduceState()
            dispatcher(dispatcher)
            reduce(reducer())
        }
        knot.start(scope)
        return knot to job
    }


    fun mviHandlerStaticIncrement(): Int {
        val (knot, job) = createStoreAndJob<IntentIncrement> {
            { intent ->
                when (intent) {
                    IntentIncrement.Increment -> copy(value = this.value + 1).stateOnly
                }
            }
        }
        return runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { knot.offerIntent(it) }
            val state = knot.state.consumeCommonBenchmark(launchDef, parentJob = job) { it.value }
            knot.stop()
            state
        }
    }

    fun mviHandlerAdd(value: Int = BENCHMARK_ARG): Int {
        val (knot, job) = createStoreAndJob<IntentAdd> {
            { intent ->
                when (intent) {
                    is IntentAdd.Add -> copy(value = this.value + intent.value).stateOnly
                }
            }
        }
        return runBlocking {
            val launchDef = launchCommonBenchmark { knot.offerIntent(IntentAdd.Add(value = value)) }
            val state = knot.state.consumeCommonBenchmark(launchDef, parentJob = job) { it.value }
            knot.stop()
            state
        }
    }


    private enum class IntentIncrement : StateIntent {
        Increment,
    }

    private sealed interface IntentAdd : StateIntent {
        data class Add(val value: Int = 1) : IntentAdd
    }

    private data class ReduceState(val value: Int = 0) : State
}
