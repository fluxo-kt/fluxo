package kt.fluxo.tests

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kt.fluxo.core.FluxoClosedException
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.updateState
import kt.fluxo.test.getValue
import kt.fluxo.test.runUnitTest
import kt.fluxo.test.setValue
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ContainerExceptionHandlerTest {

    @Test
    fun by_default_exception_breaks_the_store() = runUnitTest(dispatchTimeoutMs = 5_000) {
        val initState = 1
        var completionException by MutableStateFlow<Throwable?>(null)
        val job = Job()
        val container = container(initState) {
            /** Override the scope job to turn off error propagation in [TestScope] */
            scope = CoroutineScope(job)
            onError { completionException = it }
            closeOnExceptions = true
        }
        container.send {
            throw IllegalStateException()
        }.join()

        assertIs<IllegalStateException>(completionException, "completionException was not caught.")
        assertEquals(false, container.isActive, "Container is active but shouldn't.")

        assertFailsWith<FluxoClosedException> {
            container.send {
                updateState { 2 }
            }
        }

        assertEquals(initState, container.value)
        assertFailsWith<CancellationException> {
            assertEquals(initState, container.first())
        }

        yield() // yield so cancellation could propagate in any cases
        assertEquals(false, job.isActive, "Job is active but shouldn't.")
    }

    @Test
    fun with_exception_handler_exceptions_are_caught() = runUnitTest {
        val initState = 10
        val exceptions = mutableListOf<Throwable>()
        val container = container(initState) {
            onError { exceptions += it }
            closeOnExceptions = false
        }
        val newState = 20

        container.send {
            updateState {
                throw IllegalStateException()
            }
        }
        container.send {
            updateState {
                newState
            }
        }.join()

        assertEquals(newState, container.value)
        assertEquals(1, exceptions.size)
        assertIs<IllegalStateException>(exceptions.first(), "completionException was not caught.")

        container.closeAndWait()
    }

    @Test
    fun with_exception_handler_cancellation_exception_propagated_normally() =
        checkCancellationPropagation(withExceptionHandler = true)

    @Test
    fun without_exception_handler_cancellation_exception_propagated_normally() =
        checkCancellationPropagation(withExceptionHandler = false)

    private fun checkCancellationPropagation(withExceptionHandler: Boolean) = runUnitTest {
        val container = backgroundScope.container<Unit, Nothing>(initialState = Unit) {
            exceptionHandler = when {
                withExceptionHandler -> CoroutineExceptionHandler { _, _ -> }
                else -> null
            }
            closeOnExceptions = withExceptionHandler
        }

        val exceptions = mutableListOf<Throwable>()
        val mutex = Mutex(locked = true)
        container.send {
            try {
                mutex.unlock()
                flow {
                    while (true) {
                        emit(Unit)
                        withContext(Dispatchers.Default) {
                            delay(1000)
                        }
                    }
                }.collect()
            } catch (ce: CancellationException) {
                exceptions.add(ce)
                throw ce
            }
        }

        mutex.lock()
        container.closeAndWait()
        assertEquals(false, container.isActive, "Container is active but shouldn't.")
        assertEquals(1, exceptions.size, "CancellationException was not caught")
    }

    @Test
    @Ignore
    fun with_exception_handler_test_does_not_break() {
        TODO()
    }

    @Test
    @Ignore
    fun without_exception_handler_test_does_break() {
        TODO()
    }
}
