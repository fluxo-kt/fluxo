package kt.fluxo.test.mvikotlin

import com.arkivanov.mvikotlin.core.store.create
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.CommonBenchmark.consumeCommon
import kt.fluxo.test.CommonBenchmark.launchCommon
import kt.fluxo.test.IntentIncrement

internal object MviKotlinBenchmark {
    fun mviReducer(): Int {
        val store = DefaultStoreFactory().create<IntentIncrement, Int>(name = "CounterStore", initialState = 0) {
            when (it) {
                IntentIncrement.Increment -> this + 1
            }
        }
        runBlocking {
            val launchDef = launchCommon(IntentIncrement.Increment) { store.accept(it) }
            consumeCommon(store.states, launchDef)
        }
        store.dispose()
        return store.state
    }
}
