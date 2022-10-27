package kt.fluxo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kt.fluxo.core.ContainerHost
import kt.fluxo.core.container
import kt.fluxo.core.intent
import kt.fluxo.test.test
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
internal class ContainerLifecycleTest {

    private val scope = CoroutineScope(Job())

    @AfterTest
    fun afterTest() {
        scope.cancel()
    }

    @Test
    fun onCreate_called_once_after_connecting_to_container() = runTest {
        val initialState = TestState()
        val middleware = Middleware(initialState)
        val testStateObserver = middleware.container.stateFlow.test()
        val testSideEffectObserver = middleware.container.sideEffectFlow.test()

        testStateObserver.awaitCount(1)
        testSideEffectObserver.awaitCount(1)

        assertEquals(listOf(initialState), testStateObserver.values)
        assertEquals(listOf(initialState.id.toString()), testSideEffectObserver.values)
    }

    private data class TestState(val id: Int = Random.nextInt())

    private inner class Middleware(initialState: TestState) : ContainerHost<TestState, String> {

        override val container = scope.container<TestState, String>(initialState) {
            onCreate {
                onCreate(state)
            }
        }

        private fun onCreate(createState: TestState) = intent {
            postSideEffect(createState.id.toString())
        }
    }
}
