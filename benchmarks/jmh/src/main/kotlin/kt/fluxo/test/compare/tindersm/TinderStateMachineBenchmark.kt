package kt.fluxo.test.compare.tindersm

import com.tinder.StateMachine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent

internal object TinderStateMachineBenchmark {

    fun smReducerStaticIncrement(): Int {
        val sm = StateMachine.create<Int, IntentIncrement, Nothing> {
            initialState(0)
            state<Int> {
                on<IntentIncrement> {
                    when (it) {
                        IntentIncrement.Increment -> transitionTo(this + 1)
                    }
                }
            }
        }
        runBlocking {
            val reactiveState = MutableStateFlow(sm.state)
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) {
                val transition = sm.transition(IntentIncrement.Increment)
                require(transition is StateMachine.Transition.Valid)
                reactiveState.value = transition.toState
            }
            reactiveState.consumeCommonBenchmark(launchDef)
        }
        return sm.state
    }

    fun smReducerAdd(value: Int = BENCHMARK_ARG): Int {
        val sm = StateMachine.create<Int, IntentAdd, Nothing> {
            initialState(0)
            state<Int> {
                on<IntentAdd> {
                    when (it) {
                        is IntentAdd.Add -> transitionTo(this + it.value)
                    }
                }
            }
        }
        runBlocking {
            val reactiveState = MutableStateFlow(sm.state)
            val launchDef = launchCommonBenchmark {
                val transition = sm.transition(IntentAdd.Add(value = value))
                require(transition is StateMachine.Transition.Valid)
                reactiveState.value = transition.toState
            }
            reactiveState.consumeCommonBenchmark(launchDef)
        }
        return sm.state
    }
}
