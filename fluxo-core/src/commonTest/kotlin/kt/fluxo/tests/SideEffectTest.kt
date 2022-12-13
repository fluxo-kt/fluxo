package kt.fluxo.tests

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kt.fluxo.core.Container
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.test.runUnitTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class SideEffectTest {

    @Test
    fun side_effects_are_emitted_ordered_by_default() = runUnitTest {
        // Uses Fifo strategy by default, saving order of intents
        val container = backgroundScope.container<Unit, Int>(Unit)
        container.sideEffectFlow.test {
            repeat(1000) {
                container.postSideEffect(it)
            }
            repeat(1000) {
                assertEquals(it, awaitItem())
            }
            container.close()
            awaitComplete()
        }
        container.closeAndWait()
    }

    @Test
    fun side_effects_are_not_multicast() = runUnitTest {
        val container = backgroundScope.container<Unit, Int>(Unit)
        val result = MutableStateFlow<List<Int>>(listOf())
        val block: suspend CoroutineScope.() -> Unit = {
            container.sideEffectFlow.collect {
                do {
                    val prev = result.value
                    val next = result.value + it
                } while (!result.compareAndSet(prev, next))
            }
        }
        repeat(3) {
            backgroundScope.launch(block = block)
        }
        repeat(3) {
            container.postSideEffect(it)
        }
        val results = result.first { it.size == 3 }
        assertEquals(listOf(0, 1, 2), results)
        container.closeAndWait()
    }

    @Test
    fun side_effects_are_cached_when_there_are_no_subscribers() = runUnitTest {
        val container = backgroundScope.container<Unit, Int>(Unit)
        repeat(3) {
            container.postSideEffect(it)
        }
        assertContentEquals(listOf(0, 1, 2), container.sideEffectFlow.take(3).toList())
        container.closeAndWait()
    }

    @Test
    fun consumed_side_effects_are_not_resent() = runUnitTest {
        val container = backgroundScope.container<Unit, Int>(Unit)
        val flow = container.sideEffectFlow
        repeat(5) {
            container.postSideEffect(it)
        }
        assertContentEquals(listOf(0, 1, 2), flow.take(3).toList())
        assertContentEquals(listOf(3), flow.take(1).toList())
        container.close()

        assertContentEquals(listOf(4), flow.toList())
        container.closeAndWait()
    }

    @Test
    fun only_new_side_effects_are_emitted_when_resubscribing() = runUnitTest {
        val container = backgroundScope.container<Unit, Int>(Unit)
        val flow = container.sideEffectFlow
        container.postSideEffect(123)
        assertContentEquals(listOf(123), flow.take(1).toList())

        backgroundScope.launch {
            repeat(1000) {
                container.postSideEffect(it)
            }
        }

        assertContentEquals(0..999, flow.take(1000).toList())
        container.closeAndWait()
    }

    @Test
    fun disabled_side_effects() = runUnitTest {
        val container = container(Unit) {
            name = ""
        }
        assertFailsWith<IllegalStateException> {
            container.sideEffectFlow
        }
        container.closeAndWait()
    }

    private suspend fun Container<Unit, Int>.postSideEffect(value: Int) = send {
        postSideEffect(value)
    }
}
