package kt.fluxo.test.compare.reduktor

import g000sha256.reduktor.core.Reducer
import g000sha256.reduktor.core.common.ActionsInitializer
import g000sha256.reduktor.coroutines.Store
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent

internal object ReduktorBenchmark {
    fun mviReducerStaticIncrement(): Int {
        val (store, actionsInitializer) = createStore<IntentIncrement> {
            when (it) {
                IntentIncrement.Increment -> this + 1
            }
        }
        runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { actionsInitializer.actions.post(it) }
            store.states.consumeCommonBenchmark(launchDef)
        }
        store.release()
        return store.states.value
    }

    fun mviReducerAdd(value: Int = BENCHMARK_ARG): Int {
        val (store, actionsInitializer) = createStore<IntentAdd> {
            when (it) {
                is IntentAdd.Add -> this + it.value
            }
        }
        runBlocking {
            val launchDef = launchCommonBenchmark { actionsInitializer.actions.post(IntentAdd.Add(value = value)) }
            store.states.consumeCommonBenchmark(launchDef)
        }
        store.release()
        return store.states.value
    }

    private fun <Action> createStore(reducer: Reducer<Action, Int>): Pair<Store<Action, Int>, ActionsInitializer<Action, Int>> {
        val actionsInitializer = ActionsInitializer<Action, Int>()
        val store = Store(
            initialState = 0,
            initializers = listOf(actionsInitializer),
            reducer = reducer,
        )
        return store to actionsInitializer
    }
}
