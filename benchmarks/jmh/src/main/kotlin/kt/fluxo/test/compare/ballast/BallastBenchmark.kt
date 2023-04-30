package kt.fluxo.test.compare.ballast

import com.copperleaf.ballast.BallastViewModelConfiguration
import com.copperleaf.ballast.EventHandler
import com.copperleaf.ballast.EventHandlerScope
import com.copperleaf.ballast.InputHandler
import com.copperleaf.ballast.InputHandlerScope
import com.copperleaf.ballast.build
import com.copperleaf.ballast.core.BasicViewModel
import com.copperleaf.ballast.core.FifoInputStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent

internal object BallastBenchmark {
    @Suppress("InjectDispatcher")
    private fun <I : Any> createVmAndJob(
        reducer: suspend InputHandlerScope<I, Nothing, Int>.(input: I) -> Unit,
    ): Pair<BasicViewModel<I, Nothing, Int>, Job> {
        val dispatcher = Dispatchers.Unconfined
        val job = SupervisorJob()
        val vm = object : BasicViewModel<I, Nothing, Int>(
            coroutineScope = CoroutineScope(dispatcher + job),
            eventHandler = object : EventHandler<I, Nothing, Int> {
                override suspend fun EventHandlerScope<I, Nothing, Int>.handleEvent(event: Nothing) {}
            },
            config = BallastViewModelConfiguration.Builder().apply {
                initialState = 0
                inputStrategy = FifoInputStrategy()
                inputsDispatcher = dispatcher
                eventsDispatcher = dispatcher
                sideJobsDispatcher = dispatcher
                interceptorDispatcher = dispatcher
                inputHandler = object : InputHandler<I, Nothing, Int> {
                    override suspend fun InputHandlerScope<I, Nothing, Int>.handleInput(input: I) = reducer(input)
                }
            }.build(),
        ) {}
        return vm to job
    }

    fun mviHandlerStaticIncrement(): Int {
        val (vm, job) = createVmAndJob<IntentIncrement> { input ->
            when (input) {
                IntentIncrement.Increment -> updateState { it + 1 }
            }
        }
        return runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { vm.send(it) }
            vm.observeStates().consumeCommonBenchmark(launchDef, job)
        }
    }

    fun mviHandlerAdd(value: Int = BENCHMARK_ARG): Int {
        val (vm, job) = createVmAndJob<IntentAdd> { input ->
            when (input) {
                is IntentAdd.Add -> updateState { it + input.value }
            }
        }
        return runBlocking {
            val launchDef = launchCommonBenchmark { vm.send(IntentAdd.Add(value = value)) }
            vm.observeStates().consumeCommonBenchmark(launchDef, job)
        }
    }
}
