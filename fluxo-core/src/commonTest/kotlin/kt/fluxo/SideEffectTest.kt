package kt.fluxo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kt.fluxo.core.Container
import kt.fluxo.core.container
import kt.fluxo.test.test
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@ExperimentalCoroutinesApi
internal class SideEffectTest {

    private val scope = CoroutineScope(Job())

    @AfterTest
    fun afterTest() {
        scope.cancel()
    }

    @Test
    fun side_effects_are_emitted_in_order() = runTest {
        val container = scope.container<Unit, Int>(Unit)

        val testSideEffectObserver1 = container.sideEffectFlow.test()

        repeat(1000) {
            container.someFlow(it)
        }

        testSideEffectObserver1.awaitCount(1000, 10_000)

        assertContentEquals(0..999, testSideEffectObserver1.values)
    }

    @Test
    fun side_effects_are_not_multicast() = runTest {
        val action = Random.nextInt()
        val action2 = Random.nextInt()
        val action3 = Random.nextInt()
        val container = scope.container<Unit, Int>(Unit)

        val testSideEffectObserver1 = container.sideEffectFlow.test()
        val testSideEffectObserver2 = container.sideEffectFlow.test()
        val testSideEffectObserver3 = container.sideEffectFlow.test()

        container.someFlow(action)
        container.someFlow(action2)
        container.someFlow(action3)

        val timeout = 200L
        testSideEffectObserver1.awaitCount(3, timeout, throwTimeout = false)
        testSideEffectObserver2.awaitCount(3, timeout, throwTimeout = false)
        testSideEffectObserver3.awaitCount(3, timeout, throwTimeout = false)

        assertNotEquals(listOf(action, action2, action3), testSideEffectObserver1.values)
        assertNotEquals(listOf(action, action2, action3), testSideEffectObserver2.values)
        assertNotEquals(listOf(action, action2, action3), testSideEffectObserver3.values)
    }

    @Test
    fun side_effects_are_cached_when_there_are_no_subscribers() = runTest {
        val action = Random.nextInt()
        val action2 = Random.nextInt()
        val action3 = Random.nextInt()
        val container = scope.container<Unit, Int>(Unit)

        container.someFlow(action)
        container.someFlow(action2)
        container.someFlow(action3)

        val testSideEffectObserver1 = container.sideEffectFlow.test()

        testSideEffectObserver1.awaitCount(3)

        assertContentEquals(listOf(action, action2, action3), testSideEffectObserver1.values)
    }

    @Test
    fun consumed_side_effects_are_not_resent() = runTest {
        val action = Random.nextInt()
        val action2 = Random.nextInt()
        val action3 = Random.nextInt()
        val container = scope.container<Unit, Int>(Unit)
        val testSideEffectObserver1 = container.sideEffectFlow.test()

        container.someFlow(action)
        container.someFlow(action2)
        container.someFlow(action3)
        testSideEffectObserver1.awaitCount(3)
        testSideEffectObserver1.close()

        val testSideEffectObserver2 = container.sideEffectFlow.test()

        testSideEffectObserver1.awaitCount(3, 10L)

        assertEquals(0, testSideEffectObserver2.values.size, "should be empty")
    }

    @Test
    fun only_new_side_effects_are_emitted_when_resubscribing() = runTest {
        val action = Random.nextInt()
        val container = scope.container<Unit, Int>(Unit)

        val testSideEffectObserver1 = container.sideEffectFlow.test()

        container.someFlow(action)

        testSideEffectObserver1.awaitCount(1)
        testSideEffectObserver1.close()

        coroutineScope {
            launch {
                repeat(1000) {
                    container.someFlow(it)
                }
            }
        }

        val testSideEffectObserver2 = container.sideEffectFlow.test()
        testSideEffectObserver2.awaitCount(1000)

        assertContentEquals(listOf(action), testSideEffectObserver1.values)
        assertContentEquals(0..999, testSideEffectObserver2.values)
    }


    private suspend fun Container<Unit, Int>.someFlow(action: Int) = send {
        postSideEffect(action)
    }
}
