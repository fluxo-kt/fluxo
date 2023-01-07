package kt.fluxo.test.interception

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_REPETITIONS
import kt.fluxo.test.compare.CommonBenchmark.addAndGet
import kt.fluxo.test.interception.PipelineInterceptionChain.IntentInterceptor

/**
 * Pipeline interception as used in OkHttp, but adapted for multiple intercepted actions.
 */
object PipelineInterceptionChain {

    // https://square.github.io/okhttp/features/interceptors/
    // https://github.com/square/okhttp/blob/3ad1912/okhttp/src/jvmMain/kotlin/okhttp3/Interceptor.kt


    private sealed interface Interceptor

    private fun interface IntentInterceptor<I> : Interceptor {
        suspend fun interceptIntent(chain: InterceptorChain<I>): I
    }

    private interface InterceptorChain<R> {
        val value: R

        suspend fun proceed(value: R): R
    }

    private abstract class RealInterceptorChain<R, I : Interceptor>(
        @JvmField
        protected val interceptors: Array<out I>,
        private val index: Int,
        override val value: R,
    ) : InterceptorChain<R> {

        protected abstract fun copy(index: Int = this.index, value: R = this.value): InterceptorChain<R>

        override suspend fun proceed(value: R): R {
            check(index < interceptors.size) { "index ($index) >= interceptors.size (${interceptors.size})" }

            // Call the next interceptor in the chain.
            val next = copy(index = index + 1, value = value)
            val interceptor = interceptors[index]

            return interceptor.intercept(next) ?: throw NullPointerException("interceptor $interceptor returned null")
        }

        protected abstract suspend fun I.intercept(next: InterceptorChain<R>): R?
    }

    private class IntentInterceptorChain<I>(interceptors: Array<out IntentInterceptor<I>>, index: Int, value: I) :
        RealInterceptorChain<I, IntentInterceptor<I>>(interceptors, index, value) {

        override fun copy(index: Int, value: I) = IntentInterceptorChain(interceptors, index, value)

        override suspend fun IntentInterceptor<I>.intercept(next: InterceptorChain<I>) = interceptIntent(next)
    }

    private suspend fun getResponseWithInterceptorChain(state: MutableStateFlow<Int>): Int {
        val interceptor = IntentInterceptor<Int> { it.proceed(it.value + state.addAndGet(1)) }
        val finalInterceptor = IntentInterceptor<Int> { it.value + state.addAndGet(1) }

        // Build a full stack of interceptors

        val interceptors = Array(INTERCEPTIONS_COUNT + 1) {
            if (it == INTERCEPTIONS_COUNT) finalInterceptor else interceptor
        }

        val originalRequest = 0
        val chain = IntentInterceptorChain(interceptors, index = 0, originalRequest)
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
