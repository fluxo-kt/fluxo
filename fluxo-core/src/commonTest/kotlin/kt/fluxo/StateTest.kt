package kt.fluxo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kt.fluxo.core.ContainerHost
import kt.fluxo.core.container
import kt.fluxo.core.intent
import kt.fluxo.core.intercept.FluxoEvent
import kt.fluxo.test.test
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
internal class StateTest {

    private val scope = CoroutineScope(Job())

    @AfterTest
    fun afterTest() {
        scope.cancel()
    }

    @Test
    fun initial_state_emitted_on_connection() {
        val initialState = TestState()
        val middleware = Middleware(initialState)
        assertEquals(initialState, middleware.container.stateFlow.value)

        val testStateObserver = middleware.container.stateFlow.test()
        assertContentEquals(listOf(initialState), testStateObserver.values)
    }

    @Test
    fun latest_state_emitted_on_connection() = runTest {
        val initialState = TestState()
        val middleware = Middleware(initialState)
        val testStateObserver = middleware.container.stateFlow.test()
        val action = Random.nextInt()
        middleware.something(action)
        testStateObserver.awaitCount(2) // block until the state updated

        val testStateObserver2 = middleware.container.stateFlow.test()
        testStateObserver2.awaitCount(1)

        assertContentEquals(listOf(initialState, TestState(action)), testStateObserver.values)
        assertContentEquals(listOf(TestState(action)), testStateObserver2.values)

        middleware.container.close()
    }

    @Test
    fun current_state_up_to_date_after_modification() = runTest {
        val initialState = TestState()
        val middleware = Middleware(initialState)
        val action = Random.nextInt()
        val testStateObserver = middleware.container.stateFlow.test()

        middleware.something(action)

        testStateObserver.awaitCount(2)

        assertEquals(testStateObserver.values.last(), middleware.container.stateFlow.value)

        middleware.container.close()
    }

    private data class TestState(val id: Int = Random.nextInt())

    private inner class Middleware(
        initialState: TestState,
        scope: CoroutineScope = this.scope,
        onEvent: ((event: FluxoEvent<*, TestState, *>) -> Unit)? = null,
    ) : ContainerHost<TestState, Nothing> {
        override val container = scope.container(initialState) {
            debugChecks = true
            onEvent?.let { interceptor(it) }
        }

        fun something(action: Int) = intent {
            updateState { it.copy(id = action) }
        }
    }
}
