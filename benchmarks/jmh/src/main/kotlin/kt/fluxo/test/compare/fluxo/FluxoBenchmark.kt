package kt.fluxo.test.compare.fluxo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kt.fluxo.core.FluxoIntentS
import kt.fluxo.core.Store
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.intent.IntentStrategy
import kt.fluxo.core.internal.Closeable
import kt.fluxo.core.store
import kt.fluxo.core.updateState
import kt.fluxo.test.compare.CommonBenchmark.consumeCommon
import kt.fluxo.test.compare.CommonBenchmark.launchCommon
import kt.fluxo.test.compare.IntentIncrement
import kotlin.coroutines.CoroutineContext

@Suppress("InjectDispatcher")
internal object FluxoBenchmark {

    fun mvvmIntent(): Int {
        val container = container(0) {
            coroutineContext = Dispatchers.Unconfined
            intentStrategy = Direct
            debugChecks = false
            lazy = false
        }
        val intent: FluxoIntentS<Int> = { updateState { it + 1 } }
        return container.consumeFluxo(intent)
    }

    fun mviReducer(dispatcher: CoroutineContext? = null, strategy: IntentStrategy.Factory? = null): Int {
        var closeable: ExecutorCoroutineDispatcher? = null
        val store = store<IntentIncrement, Int>(
            initialState = 0,
            reducer = {
                when (it) {
                    IntentIncrement.Increment -> this + 1
                }
            },
        ) {
            coroutineContext = dispatcher ?: run {
                val context = newSingleThreadContext(FluxoBenchmark::mviReducer.name)
                closeable = context
                context
            }
            this.intentStrategy = strategy ?: Direct
            debugChecks = false
            lazy = false
        }
        return store.consumeFluxo(IntentIncrement.Increment, closeable)
    }

    fun mviHandler(): Int {
        val store = store<IntentIncrement, Int>(
            initialState = 0,
            handler = { intent ->
                when (intent) {
                    IntentIncrement.Increment -> updateState { it + 1 }
                }
            },
        ) {
            coroutineContext = Dispatchers.Unconfined
            intentStrategy = Direct
            debugChecks = false
            lazy = false
        }
        return store.consumeFluxo(IntentIncrement.Increment)
    }


    private fun <I> Store<I, Int>.consumeFluxo(intent: I, closeable: Closeable? = null): Int {
        runBlocking {
            val launchDef = launchCommon(intent) { send(it) }

            consumeCommon(launchDef)

            @OptIn(ExperimentalFluxoApi::class)
            closeAndWait()
        }
        closeable?.close()
        return value
    }
}
