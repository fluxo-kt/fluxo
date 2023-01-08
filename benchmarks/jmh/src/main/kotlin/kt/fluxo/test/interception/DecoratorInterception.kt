package kt.fluxo.test.interception

import kotlinx.coroutines.flow.MutableStateFlow
import kt.fluxo.test.compare.CommonBenchmark.addAndGet

/**
 * Decorator interception like in MVIKotlin (via factory) or in Orbit (via ContainerDecorator).
 * We test both decorations for outbound calls and inbound calls to the decoration chain.
 */
object DecoratorInterception : InterceptionBase() {

    // https://square.github.io/okhttp/features/interceptors/
    // https://github.com/square/okhttp/blob/3ad1912/okhttp/src/jvmMain/kotlin/okhttp3/Interceptor.kt


    private interface IntentHandler<I> {

        // Called from outside
        suspend fun intent(value: I): Int

        // Called from inside
        suspend fun sideEffect(value: I) {}

        fun init(decorator: IntentHandler<I>)
    }

    @Suppress("UnnecessaryAbstractClass")
    private abstract class IntentHandlerDecorator<I>(protected val actual: IntentHandler<I>) : IntentHandler<I> {
        override suspend fun intent(value: I) = actual.intent(value)

        override suspend fun sideEffect(value: I) = actual.sideEffect(value)

        override fun init(decorator: IntentHandler<I>) = actual.init(decorator)
    }


    override suspend fun test(interceptions: Int, interceptors: Int, state: MutableStateFlow<Int>) {
        // Build a full stack of decorators
        var handler: IntentHandler<Int> = object : IntentHandler<Int> {
            @Volatile
            private lateinit var decorator: IntentHandler<Int>

            override fun init(decorator: IntentHandler<Int>) {
                this.decorator = decorator
            }

            override suspend fun intent(value: Int): Int {
                val v = value + state.addAndGet(1)
                decorator.sideEffect(v)
                return v
            }

            override suspend fun sideEffect(value: Int) {
                state.addAndGet(value)
            }
        }
        repeat(INTERCEPTORS_COUNT) {
            handler = object : IntentHandlerDecorator<Int>(handler) {
                override suspend fun intent(value: Int) = actual.intent(it + state.addAndGet(1))
            }
        }

        // Required for inboud calls
        handler.init(handler)

        val originalRequest = 0
        check(handler.intent(originalRequest) > 0)
        check(state.value > 0)

        repeat(interceptions - 1) {
            handler.intent(originalRequest)
        }
        check(state.value != 0)
    }
}
