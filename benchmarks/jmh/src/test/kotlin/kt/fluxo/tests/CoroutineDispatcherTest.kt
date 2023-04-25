package kt.fluxo.tests

import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.test.runTest
import kt.fluxo.core.intent.IntentStrategy.InBox.Parallel
import kt.fluxo.test.compare.fluxo.FluxoBenchmark
import org.junit.Ignore
import org.junit.Test
import java.lang.System.currentTimeMillis

@Suppress("InjectDispatcher")
class CoroutineDispatcherTest {
    private companion object {
        private const val TIME_LIMIT_MILLIS = 10_000L
    }


    @Test
    fun fluxo__single_thread_context() = test {
        FluxoBenchmark.mviReducer(dispatcher = null, strategy = Parallel)
    }

    @Test
    fun fluxo__dispatchers_default() = test {
        FluxoBenchmark.mviReducer(dispatcher = Default, strategy = Parallel)
    }

    @Test
    fun fluxo__dispatchers_default_limited2() = test {
        FluxoBenchmark.mviReducer(dispatcher = Default.limitedParallelism(2), strategy = Parallel)
    }

    @Test
    fun fluxo__dispatchers_io() = test {
        FluxoBenchmark.mviReducer(dispatcher = IO, strategy = Parallel)
    }

    @Test
    fun fluxo__dispatchers_unconfined() = test {
        FluxoBenchmark.mviReducer(dispatcher = Unconfined, strategy = Parallel)
    }

    @Test
    @Ignore // TODO: fix freeze
    fun fluxo__test_dispatcher() = runTest {
        FluxoBenchmark.mviReducer(dispatcher = coroutineContext, strategy = Parallel)
    }

    @Test
    @Ignore // TODO: fix freeze
    fun fluxo__background_test_dispatcher() = runTest {
        FluxoBenchmark.mviReducer(dispatcher = backgroundScope.coroutineContext, strategy = Parallel)
    }


    private inline fun test(callback: () -> Unit) {
        val start = currentTimeMillis()
        while (currentTimeMillis() - start < TIME_LIMIT_MILLIS) {
            callback()
        }
    }
}
