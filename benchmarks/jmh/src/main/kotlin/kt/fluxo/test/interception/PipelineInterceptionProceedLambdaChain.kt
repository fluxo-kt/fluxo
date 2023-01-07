package kt.fluxo.test.interception

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_REPETITIONS
import kt.fluxo.test.compare.CommonBenchmark.addAndGet
import kt.fluxo.test.compare.CommonBenchmark.getAndAdd
import kt.fluxo.test.interception.PipelineInterceptionProceedLambdaChain.Interceptor

object PipelineInterceptionProceedLambdaChain {

    private fun interface Interceptor<R> {

        suspend fun intent(request: R, proceed: suspend (request: R) -> Unit)
    }


    private class InterceptorChain<R>(
        value: R,
        private val interceptors: Array<out Interceptor<R>>,
        private val call: suspend Interceptor<R>.(R, proceed: suspend (R) -> Unit) -> Unit,
        private val finish: suspend (finalValue: R) -> Unit,
    ) {
        private val index = MutableStateFlow(0)
        private val value = MutableStateFlow(value)

        suspend fun proceed() {
            val i = index.value
            if (i >= interceptors.size) {
                finish(value.value)
            } else {
                interceptors[i].run {
                    call(value.value) { v ->
                        value.value = v
                        index.getAndAdd(delta = 1)
                        proceed()
                    }
                }
            }
        }
    }

    private suspend fun getResponseWithInterceptorChain(state: MutableStateFlow<Int>) {
        val interceptor = Interceptor<Int> { request, proceed ->
            proceed(request + state.addAndGet(1))
        }
        val originalRequest = 0
        val interceptors = Array(INTERCEPTIONS_COUNT) { interceptor }
        InterceptorChain(
            originalRequest, interceptors,
            { value, proceed -> intent(value, proceed) },
        ) { state.addAndGet(1) }.proceed()
    }


    fun test() = runBlocking {
        val state = MutableStateFlow(0)
        repeat(BENCHMARK_REPETITIONS) {
            state.value = 0
            getResponseWithInterceptorChain(state)
            check(state.value == INTERCEPTIONS_COUNT + 1) { "state.value=${state.value}, expected=${INTERCEPTIONS_COUNT + 1}" }
        }
    }
}
