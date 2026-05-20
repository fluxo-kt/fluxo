package kt.fluxo.test.compare.elmslie

import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent
import money.vivid.elmslie.core.config.ElmslieConfig
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.NoOpActor
import money.vivid.elmslie.core.store.StateReducer
import money.vivid.elmslie.core.store.Store

internal object ElmslieBenchmark {

    private fun <Event : Any> createStore(reducer: StateReducer<Event, Int, Nothing, Nothing>): Store<Event, Nothing, Int> {
        return ElmStore(
            initialState = 0,
            reducer = reducer,
            actor = NoOpActor()
        ).start()
    }

    fun elmReducerStaticIncrement(): Int {
        val store = createStore(object : StateReducer<IntentIncrement, Int, Nothing, Nothing>() {
            override fun Result.reduce(event: IntentIncrement) {
                when (event) {
                    IntentIncrement.Increment -> state { this + 1 }
                }
            }
        })
        runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { store.accept(it) }
            store.states.consumeCommonBenchmark(launchDef)
            store.stop()
        }
        return store.states.value
    }

    fun elmReducerAdd(value: Int = BENCHMARK_ARG): Int {
        val store = createStore(object : StateReducer<IntentAdd, Int, Nothing, Nothing>() {
            override fun Result.reduce(event: IntentAdd) {
                when (event) {
                    is IntentAdd.Add -> state { this + event.value }
                }
            }
        })
        runBlocking {
            val launchDef = launchCommonBenchmark { store.accept(IntentAdd.Add(value = value)) }
            store.states.consumeCommonBenchmark(launchDef)
            store.stop()
        }
        return store.states.value
    }
}
