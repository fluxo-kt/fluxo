package kt.fluxo.test.compare.reduxkotlin

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.BENCHMARK_REPETITIONS
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent
import org.reduxkotlin.TypedStore
import org.reduxkotlin.threadsafe.createTypedThreadSafeStore
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal object ReduxKotlinBenchmark {

    fun mviReducerStaticIncrement(): Int {
        val store = createTypedThreadSafeStore(reducer = { state: Int, action: IntentIncrement ->
            when (action) {
                IntentIncrement.Increment -> state + 1
            }
        }, preloadedState = 0)
        runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { store.dispatch(it) }
            store.consumeReduxKotlinBenchmark(launchDef)
        }
        return store.state
    }

    fun mviReducerAdd(value: Int = BENCHMARK_ARG): Int {
        val store = createTypedThreadSafeStore({ state: Int, action: IntentAdd ->
            when (action) {
                is IntentAdd.Add -> state + action.value
            }
        }, preloadedState = 0)
        runBlocking {
            val launchDef = launchCommonBenchmark { store.dispatch(IntentAdd.Add(value = value)) }
            store.consumeReduxKotlinBenchmark(launchDef)
        }
        return store.state
    }


    private suspend fun TypedStore<Int, *>.consumeReduxKotlinBenchmark(launchDef: Job): Int {
        val state = suspendCoroutine { cont ->
            subscribe {
                val state = state
                if (state >= BENCHMARK_REPETITIONS) {
                    cont.resume(state)
                }
            }
        }
        launchDef.join()
        return state
    }
}
