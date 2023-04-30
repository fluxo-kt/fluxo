package kt.fluxo.test.compare.mvikotlin

import com.arkivanov.mvikotlin.core.store.create
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent
import java.io.OutputStream
import java.io.PrintStream
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal object MviKotlinBenchmark {

    // https://github.com/arkivanov/MVIKotlin/blob/57ade7a/sample/coroutines/shared/src/commonMain/kotlin/com/arkivanov/mvikotlin/sample/coroutines/shared/details/store/DetailsStoreFactory.kt

    fun mviReducerStaticIncrement(): Int = devNullErrorOutput {
        val store = DefaultStoreFactory().create<IntentIncrement, Int>(name = "CounterStore", initialState = 0) {
            when (it) {
                IntentIncrement.Increment -> this + 1
            }
        }
        runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { store.accept(it) }
            store.states.consumeCommonBenchmark(launchDef)
        }
        store.dispose()
        store.state
    }

    fun mviReducerAdd(value: Int = BENCHMARK_ARG): Int = devNullErrorOutput {
        val store = DefaultStoreFactory().create<IntentAdd, Int>(name = "CounterStore", initialState = 0) {
            when (it) {
                is IntentAdd.Add -> this + it.value
            }
        }
        runBlocking {
            val launchDef = launchCommonBenchmark { store.accept(IntentAdd.Add(value = value)) }
            store.states.consumeCommonBenchmark(launchDef)
        }
        store.dispose()
        store.state
    }


    private inline fun devNullErrorOutput(block: () -> Int): Int {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        // Override stderr to get rid of `[MVIKotlin]: Main thread ID is undefined, main thread assert is disabled`.
        val stderr = System.err
        try {
            System.setErr(PrintStream(OutputStream.nullOutputStream()))
            return block()
        } finally {
            System.setErr(stderr)
        }
    }
}
