package kt.fluxo.test.compare.elmslie

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent
import vivid.money.elmslie.core.config.ElmslieConfig
import vivid.money.elmslie.core.store.ElmStore
import vivid.money.elmslie.core.store.NoOpActor
import vivid.money.elmslie.core.store.Result
import vivid.money.elmslie.core.store.StateReducer
import vivid.money.elmslie.core.store.Store
import vivid.money.elmslie.coroutines.states

internal object ElmslieBenchmark {

    @Suppress("InjectDispatcher")
    private fun <Event : Any> createStore(reducer: StateReducer<Event, Int, Nothing, Nothing>): Store<Event, Nothing, Int> {
        ElmslieConfig.ioDispatchers { Dispatchers.Unconfined }
        return ElmStore(
            initialState = 0,
            reducer = reducer,
            actor = NoOpActor()
        ).start()
    }

    fun elmReducerStaticIncrement(): Int {
        val store = createStore { event: IntentIncrement, state: Int ->
            when (event) {
                IntentIncrement.Increment -> {
                    // Not enough information to infer type variable F
                    @Suppress("RemoveExplicitTypeArguments")
                    Result<Int, Nothing, Nothing>(state = state + 1)
                }
            }
        }
        runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { store.accept(it) }
            store.states.consumeCommonBenchmark(launchDef)
            store.stop()
        }
        return store.currentState
    }

    fun elmReducerAdd(value: Int = BENCHMARK_ARG): Int {
        val store = createStore { event: IntentAdd, state: Int ->
            when (event) {
                is IntentAdd.Add -> {
                    // Not enough information to infer type variable F
                    @Suppress("RemoveExplicitTypeArguments")
                    Result<Int, Nothing, Nothing>(state + event.value)
                }
            }
        }
        runBlocking {
            val launchDef = launchCommonBenchmark { store.accept(IntentAdd.Add(value = value)) }
            store.states.consumeCommonBenchmark(launchDef)
            store.stop()
        }
        return store.currentState
    }
}
