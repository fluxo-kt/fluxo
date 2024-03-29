package kt.fluxo.test.compare.respawnflowmvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent
import pro.respawn.flowmvi.api.ActionShareBehavior
import pro.respawn.flowmvi.api.DelicateStoreApi
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.Reduce
import pro.respawn.flowmvi.plugins.reduce
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal object RespawnFlowMviBenchmark {

    @Suppress("InjectDispatcher")
    private fun <I : MVIIntent> createStoreAndJob(
        @BuilderInference reduce: Reduce<RespawnFlowMviState, I, Nothing>,
    ): Pair<Store<RespawnFlowMviState, I, Nothing>, Job> {
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Unconfined + job)
        val store = store(RespawnFlowMviState()) {
            actionShareBehavior = ActionShareBehavior.Disabled
            debuggable = false
            reduce(reduce = reduce)
        }
        store.start(scope)
        return store to job
    }


    fun mviHandlerStaticIncrement(): Int {
        val (store, job) = createStoreAndJob<IntentIncrement> {
            @OptIn(DelicateStoreApi::class)
            when (it) {
                // The default option is to use updateState, but it's pretty slow (uses lock, calls plugins, etc.).
                // `useState` is exactly for performance-critical cases.
                IntentIncrement.Increment -> useState { copy(value = value + 1) }
            }
        }
        return runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { store.send(it) }
            suspendCoroutine { cont ->
                with(store) {
                    subscribe {
                        cont.resume(states.consumeCommonBenchmark(launchDef, job) { it.value })
                    }
                }
            }
        }
    }

    fun mviHandlerAdd(value: Int = BENCHMARK_ARG): Int {
        val (store, job) = createStoreAndJob<IntentAdd> {
            when (it) {
                is IntentAdd.Add -> updateState { copy(value = this.value + it.value) }
            }
        }
        return runBlocking {
            val launchDef = launchCommonBenchmark { store.send(IntentAdd.Add(value = value)) }
            suspendCoroutine { cont ->
                with(store) {
                    subscribe {
                        cont.resume(states.consumeCommonBenchmark(launchDef, job) { it.value })
                    }
                }
            }
        }
    }


    private enum class IntentIncrement : MVIIntent {
        Increment,
    }

    private sealed interface IntentAdd : MVIIntent {
        data class Add(val value: Int = 1) : IntentAdd
    }

    private data class RespawnFlowMviState(val value: Int = 0) : MVIState
}
