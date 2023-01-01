package kt.fluxo.test.mvicore

import com.badoo.mvicore.feature.ReducerFeature
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.BENCHMARK_REPETITIONS
import kt.fluxo.test.CommonBenchmark.launchCommon
import kt.fluxo.test.IntentIncrement
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


internal object MviCoreBenchmark {
    fun mviReducer(): Int {
        val feature = ReducerFeature<IntentIncrement, Int, Nothing>(
            initialState = 0,
            reducer = { state, wish ->
                when (wish) {
                    IntentIncrement.Increment -> state + 1
                }
            },
        )
        runBlocking {
            val launchDef = launchCommon(IntentIncrement.Increment) { feature.accept(it) }
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
            launchDef.join()
        }
        feature.dispose()
        return feature.state
    }
}
