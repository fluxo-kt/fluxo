package kt.fluxo

import app.cash.turbine.testIn
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kt.fluxo.core.StoreClosedException
import kt.fluxo.core.container
import kt.fluxo.test.KMM_PLATFORM
import kt.fluxo.test.Platform
import kt.fluxo.test.mayFailWith
import kt.fluxo.test.unitTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.AfterTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class ContainerExceptionHandlerTest {

    private val scope = CoroutineScope(Job() + CoroutineExceptionHandler { _, _ -> /*just be silent*/ })

    @AfterTest
    fun afterTest() {
        scope.cancel()
    }

    @Test
    fun by_default_exception_breaks_the_scope() = unitTest {
        val initState = 10
        val container = scope.container(initState)
        val newState = 20

        var completionException: Throwable? = null
        scope.coroutineContext[Job]?.invokeOnCompletion {
            completionException = it
        }

        container.send {
            throw IllegalStateException()
        }

        mayFailWith<StoreClosedException> {
            container.send {
                updateState { newState }
            }
        }

        assertFailsWith<CancellationException> {
            container.stateFlow.testIn(scope).run {
                assertEquals(initState, awaitItem())
                awaitComplete()
            }
        }

        assertEquals(false, scope.isActive, "Scope is active but shouldn't.")
        if (KMM_PLATFORM != Platform.MINGW) {
            assertIs<IllegalStateException>(completionException, "completionException was not caught.")
        } else {
            assertIs<IllegalStateException?>(completionException, "completionException was not caught.")
        }
    }

    @Test
    fun with_exception_handler_exceptions_are_caught() = unitTest {
        val initState = 10
        val exceptions = mutableListOf<Throwable>()
        val handler = CoroutineExceptionHandler { _, throwable -> exceptions += throwable }
        val container = scope.container(initState) {
            exceptionHandler = handler
            intentContext = Dispatchers.Unconfined
        }
        val newState = 20

        container.send {
            updateState {
                throw IllegalStateException()
            }
        }
        container.sendAsync {
            updateState {
                newState
            }
        }.join()

        assertEquals(newState, container.state)
        assertEquals(true, scope.isActive)
        assertEquals(1, exceptions.size)
        assertTrue { exceptions.first() is IllegalStateException }
    }

    @Test
    fun with_exception_handler_cancellation_exception_propagated_normally() =
        checkCancellationPropagation(withExceptionHandler = true)

    @Test
    fun without_exception_handler_cancellation_exception_propagated_normally() =
        checkCancellationPropagation(withExceptionHandler = false)

    private fun checkCancellationPropagation(withExceptionHandler: Boolean) = unitTest {
        val scopeJob = SupervisorJob()
        val containerScope = CoroutineScope(scopeJob)
        val handler = when {
            withExceptionHandler -> CoroutineExceptionHandler { _, _ -> }
            else -> null
        }
        val container = containerScope.container<Unit, Nothing>(initialState = Unit) {
            exceptionHandler = handler
            intentContext = Dispatchers.Unconfined
        }

        // required on JS to pass this test
        container.start()?.join()

        val exceptions = mutableListOf<Throwable>()
        container.send {
            try {
                flow {
                    while (true) {
                        emit(Unit)
                        withContext(Dispatchers.Default) {
                            delay(1000)
                        }
                    }
                }.collect()
            } catch (e: CancellationException) {
                exceptions.add(e)
                throw e
            }
        }
        containerScope.cancel()
        scopeJob.join()
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
