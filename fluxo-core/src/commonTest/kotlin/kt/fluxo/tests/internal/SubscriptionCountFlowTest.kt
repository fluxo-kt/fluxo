package kt.fluxo.tests.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kt.fluxo.core.internal.SubscriptionCountFlow
import kt.fluxo.test.runUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionCountFlowTest {

    @Test
    fun increments_on_collection_and_decrements_once_completed() = runUnitTest {
        val subscribedCounter = MutableStateFlow(0)

        assertEquals(0, subscribedCounter.value)

        flowOf(Unit).refCount(subscribedCounter).take(1).collect {
            assertEquals(1, subscribedCounter.value)
        }

        assertEquals(0, subscribedCounter.value)
    }

    @Test
    fun decrements_even_after_exception() = runUnitTest {
        val subscribedCounter = MutableStateFlow(0)

        try {
            flowOf(Unit).refCount(subscribedCounter).take(1).collect {
                assertEquals(1, subscribedCounter.value)
                error("forced exception")
            }
        } catch (_: IllegalStateException) {
            // ignored as we just care that counter is decremented
        }

        assertEquals(0, subscribedCounter.value)
    }


    private fun <T> Flow<T>.refCount(subscribedCounter: MutableStateFlow<Int>): Flow<T> =
        SubscriptionCountFlow(subscribedCounter, this)
}
