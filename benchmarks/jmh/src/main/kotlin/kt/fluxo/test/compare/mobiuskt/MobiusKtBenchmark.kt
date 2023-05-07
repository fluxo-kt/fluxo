package kt.fluxo.test.compare.mobiuskt

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.BENCHMARK_REPETITIONS
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent
import kt.mobius.MobiusLoop
import kt.mobius.Next.Companion.next
import kt.mobius.Update
import kt.mobius.flow.FlowMobius
import kt.mobius.flow.FlowTransformer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal object MobiusKtBenchmark {
    private fun <Event> createAndStartLoop(update: Update<Int, Event, Nothing>): MobiusLoop<Int, Event, Nothing> {
        val effectHandler = FlowTransformer<Nothing, Event> { _ -> flowOf() }
        val loopFactory = FlowMobius.loop(update = update, effectHandler = effectHandler)
        return loopFactory.startFrom(startModel = 0)
    }

    fun smReducerStaticIncrement(): Int {
        val loop = createAndStartLoop<IntentIncrement> { model, event ->
            when (event) {
                IntentIncrement.Increment -> {
                    // Not enough information to infer type variable F
                    @Suppress("RemoveExplicitTypeArguments")
                    next<Int, Nothing>(model + 1)
                }
            }
        }
        return runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) {
                loop.dispatchEvent(IntentIncrement.Increment)
            }
            consumeMobiusKtBenchmark(loop, launchDef)
        }
    }

    fun smReducerAdd(value: Int = BENCHMARK_ARG): Int {
        val loop = createAndStartLoop<IntentAdd> { model, event ->
            when (event) {
                is IntentAdd.Add -> {
                    // Not enough information to infer type variable F
                    @Suppress("RemoveExplicitTypeArguments")
                    next<Int, Nothing>(model + event.value)
                }
            }
        }
        return runBlocking {
            val launchDef = launchCommonBenchmark {
                loop.dispatchEvent(IntentAdd.Add(value = value))
            }
            consumeMobiusKtBenchmark(loop, launchDef)
        }
    }

    private suspend fun consumeMobiusKtBenchmark(loop: MobiusLoop<Int, *, *>, launchDef: Job): Int {
        suspendCoroutine { cont ->
            loop.observe {
                if (it >= BENCHMARK_REPETITIONS) {
                    cont.resume(it)
                }
            }
        }
        loop.dispose()
        launchDef.join()
        return loop.mostRecentModel
    }
}
