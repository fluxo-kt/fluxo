package kt.fluxo.test.compare.fluxo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kt.fluxo.core.FluxoIntentS
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.Store
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.intent.IntentStrategy
import kt.fluxo.core.internal.Closeable
import kt.fluxo.core.store
import kt.fluxo.core.updateState
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent
import kotlin.coroutines.CoroutineContext

@Suppress("InjectDispatcher")
internal object FluxoBenchmark {
    private val storeBenchmarkSettings: FluxoSettings<*, Int, Nothing>.() -> Unit = {
        coroutineContext = Dispatchers.Unconfined
        intentStrategy = Direct
        debugChecks = false
        lazy = false
    }


    fun mvvmpIntentStaticIncrement(): Int {
        val container = container(0) {
            coroutineContext = Dispatchers.Unconfined
            intentStrategy = Direct
            debugChecks = false
            lazy = false
        }
        val intent: FluxoIntentS<Int> = { updateState { it + 1 } }
        return container.consumeFluxoWithStaticIntent(intent)
    }

    fun mvvmpIntentAdd(value: Int = BENCHMARK_ARG): Int {
        val container = container(initialState = 0, setup = storeBenchmarkSettings)
        return container.consumeFluxo {
            { updateState { it + value } }
        }
    }

    fun mviReducerStaticIncrement(dispatcher: CoroutineContext? = null, strategy: IntentStrategy.Factory? = null): Int {
        var closeable: ExecutorCoroutineDispatcher? = null
        val store = store<IntentIncrement, Int>(
            initialState = 0,
            reducer = {
                when (it) {
                    IntentIncrement.Increment -> this + 1
                }
            },
        ) {
            storeBenchmarkSettings()
            coroutineContext = dispatcher ?: run {
                val context = newSingleThreadContext(FluxoBenchmark::mviReducerStaticIncrement.name)
                closeable = context
                context
            }
            if (strategy != null) this.intentStrategy = strategy
        }
        return store.consumeFluxoWithStaticIntent(IntentIncrement.Increment, closeable)
    }

    fun mviReducerAdd(value: Int = BENCHMARK_ARG): Int {
        val store = store<IntentAdd, Int>(
            initialState = 0,
            reducer = {
                when (it) {
                    is IntentAdd.Add -> this + it.value
                }
            },
            setup = storeBenchmarkSettings,
        )
        return store.consumeFluxo { IntentAdd.Add(value = value) }
    }

    fun mviHandlerStaticIncrement(): Int {
        val store = store<IntentIncrement, Int>(
            initialState = 0,
            handler = { intent ->
                when (intent) {
                    IntentIncrement.Increment -> updateState { it + 1 }
                }
            },
            setup = storeBenchmarkSettings,
        )
        return store.consumeFluxoWithStaticIntent(IntentIncrement.Increment)
    }

    fun mviHandlerAdd(value: Int = BENCHMARK_ARG): Int {
        val store = store<IntentAdd, Int>(
            initialState = 0,
            handler = { intent ->
                when (intent) {
                    is IntentAdd.Add -> updateState { it + intent.value }
                }
            },
            setup = storeBenchmarkSettings,
        )
        return store.consumeFluxo { IntentAdd.Add(value = value) }
    }


    private fun <I> Store<I, Int>.consumeFluxo(getIntent: () -> I): Int {
        runBlocking {
            val launchDef = launchCommonBenchmark { emit(getIntent()) }

            consumeCommonBenchmark(launchDef)

            @OptIn(ExperimentalFluxoApi::class)
            closeAndWait()
        }
        return value
    }

    private fun <I> Store<I, Int>.consumeFluxoWithStaticIntent(intent: I, closeable: Closeable? = null): Int {
        runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(intent) { emit(it) }

            consumeCommonBenchmark(launchDef)

            @OptIn(ExperimentalFluxoApi::class)
            closeAndWait()
        }
        closeable?.close()
        return value
    }
}
