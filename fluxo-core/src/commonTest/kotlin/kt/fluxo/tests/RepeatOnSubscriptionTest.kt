package kt.fluxo.tests

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.yield
import kt.fluxo.core.Container
import kt.fluxo.core.SideEffectsStrategy
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.dsl.ContainerHostS
import kt.fluxo.core.intent
import kt.fluxo.core.repeatOnSubscription
import kt.fluxo.core.updateState
import kt.fluxo.test.CoroutineScopeAwareTest
import kt.fluxo.test.IgnoreNative
import kt.fluxo.test.runUnitTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.fail

internal class RepeatOnSubscriptionTest : CoroutineScopeAwareTest() {

    private val initialState = State()

    @Test
    fun repeatOnSubscription_mechanics() = runUnitTest {
        val sideEffectsStrategies = arrayOf(
            SideEffectsStrategy.RECEIVE,
            SideEffectsStrategy.CONSUME,
            SideEffectsStrategy.SHARE(),
            SideEffectsStrategy.DISABLE,
        )

        /** [TestScope] disables actual delay waiting */
        for (stopTimeout in longArrayOf(0, 5000)) {
            for (scope in arrayOf(this, backgroundScope)) {
                sideEffectsStrategies.forEach { seStrategy ->
                    val container = scope.container<State, Int>(initialState) {
                        onStart {
                            repeatOnSubscription(stopTimeout = stopTimeout) {
                                updateState { it.copy(count = it.count + 1) }
                                if (seStrategy != SideEffectsStrategy.DISABLE) {
                                    postSideEffect(value.count)
                                }
                            }
                        }
                        sideEffectsStrategy = seStrategy
                    }
                    repeatOnSubscription_mechanics0(container, seStrategy, stopTimeout)
                }
            }
        }
    }

    private suspend fun repeatOnSubscription_mechanics0(store: Container<State, Int>, strategy: SideEffectsStrategy, timeout: Long) {
        assertEquals(initialState, store.value)

        val states = store.take(2).toList()
        assertContentEquals(listOf(initialState, State(1)), states)

        // NOTE: time-skipping delay here!
        delay(timeout)
        yield()
        assertEquals(State(1), store.value)

        val states2 = store.take(2).toList()
        assertContentEquals(listOf(State(1), State(2)), states2)

        // NOTE: time-skipping delay here!
        delay(timeout)
        yield()
        assertEquals(State(2), store.value)

        when (strategy) {
            SideEffectsStrategy.RECEIVE, SideEffectsStrategy.CONSUME -> {
                val effects = store.sideEffectFlow.take(3).toList()
                assertContentEquals(listOf(1, 2, 3), effects, "SideEffectsStrategy: $strategy")
                assertEquals(State(3), store.value)
            }

            is SideEffectsStrategy.SHARE -> {
                val effects = store.sideEffectFlow.take(1).toList()
                assertContentEquals(listOf(3), effects, "SideEffectsStrategy: $strategy")
                assertEquals(State(3), store.value)
            }

            SideEffectsStrategy.DISABLE -> {
                // side effects turned off
                assertFailsWith<IllegalStateException> { store.sideEffectFlow }
            }

            else -> fail("Unexpected SideEffectsStrategy: $strategy")
        }

        store.closeAndWait()
    }


    @Test
    @IgnoreNative
    fun test_does_not_hang_when_using_repeatOnSubscription() = runUnitTest {
        val host = TestMiddleware()

        assertEquals(initialState, host.container.value)

        host.callOnSubscription { 42 }

        assertEquals(initialState, host.container.value)

        val states = host.container.take(2).toList()
        assertContentEquals(listOf(initialState, State(42)), states)

        host.container.closeAndWait()
    }

    private inner class TestMiddleware : ContainerHostS<State> {
        override val container = scope.container<State, Nothing>(initialState)

        fun callOnSubscription(externalCall: suspend () -> Int) = intent {
            repeatOnSubscription { wasRestarted ->
                assertFalse(wasRestarted)
                val result = externalCall()
                value = State(result)
            }
        }
    }

    private data class State(val count: Int = 0)
}
