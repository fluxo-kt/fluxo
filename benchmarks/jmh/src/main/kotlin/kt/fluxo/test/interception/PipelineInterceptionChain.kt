package kt.fluxo.test.interception

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_REPETITIONS
import kt.fluxo.test.compare.CommonBenchmark.addAndGet
import kt.fluxo.test.interception.PipelineInterceptionChain.Interceptor

object PipelineInterceptionChain {

    // https://square.github.io/okhttp/features/interceptors/
    // https://github.com/square/okhttp/blob/3ad1912/okhttp/src/jvmMain/kotlin/okhttp3/Interceptor.kt

    private fun interface Interceptor {
        suspend fun intercept(chain: Chain): Int

        interface Chain {
            val request: Int

            suspend fun proceed(request: Int): Int
        }
    }

    private class RealInterceptorChain(
        private val interceptors: List<Interceptor>,
        private val index: Int,
        override val request: Int,
    ) : Interceptor.Chain {

        private var calls: Int = 0

        private fun copy(index: Int = this.index, request: Int = this.request) = RealInterceptorChain(interceptors, index, request)

        override suspend fun proceed(request: Int): Int {
            check(index < interceptors.size) { "index ($index) >= interceptors.size (${interceptors.size})" }

            calls++

            // Call the next interceptor in the chain.
            val next = copy(index = index + 1, request = request)
            val interceptor = interceptors[index]

            @Suppress("USELESS_ELVIS")
            return interceptor.intercept(next) ?: throw NullPointerException("interceptor $interceptor returned null")
        }
    }

    private suspend fun getResponseWithInterceptorChain(state: MutableStateFlow<Int>): Int {
        val interceptor = Interceptor { it.proceed(it.request + state.addAndGet(1)) }
        val finalInterceptor = Interceptor { it.request + state.addAndGet(1) }

        // Build a full stack of interceptors

        val interceptors = Array(INTERCEPTIONS_COUNT + 1) {
            if (it == INTERCEPTIONS_COUNT) finalInterceptor else interceptor
        }.asList()

        val originalRequest = 0
        val chain = RealInterceptorChain(
            interceptors = interceptors,
            index = 0,
            request = originalRequest,
        )
        return chain.proceed(originalRequest)
    }


    fun test(): Int = runBlocking {
        var prev = -1
        val state = MutableStateFlow(0)
        repeat(BENCHMARK_REPETITIONS) {
            state.value = 0
            val result = getResponseWithInterceptorChain(state)

            check(state.value == INTERCEPTIONS_COUNT + 1) { "state.value=${state.value}, expected=${INTERCEPTIONS_COUNT + 1}" }

            if (prev == -1) {
                prev = result
            } else {
                // should have constant results in each iteration
                check(result == prev) { "result=$result, prev=$prev" }
            }
        }
        prev
    }
}
