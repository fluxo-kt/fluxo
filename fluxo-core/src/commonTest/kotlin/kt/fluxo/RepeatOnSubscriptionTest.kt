package kt.fluxo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kt.fluxo.core.ContainerHost
import kt.fluxo.core.container
import kt.fluxo.core.intent
import kt.fluxo.core.repeatOnSubscription
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

internal class RepeatOnSubscriptionTest {
    private val initialState = State()
    private val scope = CoroutineScope(Job())

    @AfterTest
    fun afterTest() {
        scope.cancel()
    }

    @Test
    fun test_does_not_hang_when_using_repeatOnSubscription() = runTest {
        val host = TestMiddleware()

        assertEquals(initialState, host.container.state)

        withContext(Dispatchers.Default) {
            withTimeout(timeMillis = 1000L) {
                host.callOnSubscription { 42 }
            }
        }

        assertEquals(initialState, host.container.state)

        val states = host.container.stateFlow.take(2).toList()
        assertContentEquals(listOf(initialState, State(42)), states)
    }

    private inner class TestMiddleware : ContainerHost<State, Nothing> {
        override val container = scope.container<State, Nothing>(initialState)

        fun callOnSubscription(externalCall: suspend () -> Int) = intent {
            repeatOnSubscription {
                val result = externalCall()
                updateState { State(result) }
            }
        }
    }

    private data class State(val count: Int = 0)
}
