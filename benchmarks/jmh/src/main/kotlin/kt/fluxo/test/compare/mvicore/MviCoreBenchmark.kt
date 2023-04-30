package kt.fluxo.test.compare.mvicore

import com.badoo.mvicore.feature.ReducerFeature
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.BENCHMARK_REPETITIONS
import kt.fluxo.test.compare.IntentAdd
import kt.fluxo.test.compare.IntentIncrement
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal object MviCoreBenchmark {
    fun mviReducerStaticIncrement(): Int {
        val feature = ReducerFeature<IntentIncrement, Int, Nothing>(
            initialState = 0,
            reducer = { state, wish ->
                when (wish) {
                    IntentIncrement.Increment -> state + 1
                }
            },
        )
        runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { feature.accept(it) }
            consumeBallastBenchmark(feature)
            launchDef.join()
        }
        feature.dispose()
        return feature.state
    }

    fun mviReducerAdd(value: Int = BENCHMARK_ARG): Int {
        val feature = ReducerFeature<IntentAdd, Int, Nothing>(
            initialState = 0,
            reducer = { state, wish ->
                when (wish) {
                    is IntentAdd.Add -> state + wish.value
                }
            },
        )
        runBlocking {
            val launchDef = launchCommonBenchmark { feature.accept(IntentAdd.Add(value = value)) }
            consumeBallastBenchmark(feature)
            launchDef.join()
        }
        feature.dispose()
        return feature.state
    }

    private suspend fun consumeBallastBenchmark(feature: ReducerFeature<*, Int, Nothing>) {
        suspendCoroutine { cont ->
            @Suppress("TrailingCommaOnCallSite")
            feature.subscribe(
                observer = object : Observer<Int> {
                    override fun onNext(state: Int) {
                        if (state >= BENCHMARK_REPETITIONS) {
                            cont.resume(state)
                        }
                    }

                    override fun onComplete() {}
                    override fun onError(e: Throwable) {}
                    override fun onSubscribe(d: Disposable) {}
                },
            )
        }
    }
}
