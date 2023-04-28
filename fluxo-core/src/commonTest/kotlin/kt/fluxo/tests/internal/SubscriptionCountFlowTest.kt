package kt.fluxo.tests.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kt.fluxo.core.internal.SubscriptionCountFlow
import kt.fluxo.core.internal.plus
import kt.fluxo.test.IgnoreJs
import kt.fluxo.test.runUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionCountFlowTest {

    @Test
    fun state_flow_check() = runUnitTest {
        val stateFlow = MutableStateFlow(0)
        checkFlowAndSubscribedCounter(stateFlow.subscriptionCount, stateFlow)
    }

    @Test
    fun shared_flow_check() = runUnitTest {
        val sharedFlow = MutableSharedFlow<Unit>(1)
        sharedFlow.emit(Unit)
        checkFlowAndSubscribedCounter(sharedFlow.subscriptionCount, sharedFlow)
    }

    private suspend fun checkFlowAndSubscribedCounter(subscribedCounter: StateFlow<Int>, flow: Flow<*>) {
        assertEquals(0, subscribedCounter.value)
        assertEquals(0, subscribedCounter.first())

        flow.take(1).collect {
            assertEquals(1, subscribedCounter.value)
            assertEquals(1, subscribedCounter.first())
        }

        assertEquals(0, subscribedCounter.value)
        assertEquals(0, subscribedCounter.first())
    }


    @Test
    fun increments_on_collection_and_decrements_once_completed() = runUnitTest {
        val subscribedCounter = MutableStateFlow(0)
        checkFlowAndSubscribedCounter(subscribedCounter, flowOf(Unit).refCount(subscribedCounter))
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


    // TODO: Timeout of 2000ms exceeded
    //  :jsNodeTest win CI
    //  https://github.com/fluxo-kt/fluxo/actions/runs/4795937502/jobs/8531106215#step:8:1185
    @Test
    @IgnoreJs
    fun combined_counters() = runUnitTest {
        val counter1 = MutableStateFlow(0)
        val counter2 = MutableStateFlow(0)
        val counter3 = MutableStateFlow(0)

        val flow1 = flowOf(Unit).refCount(counter1)
        val flow2 = flowOf(Unit).refCount(counter2)
        val flow3 = flowOf(Unit).refCount(counter2)

        var combinedCounter = counter1 + counter2
        checkFlowAndSubscribedCounter(combinedCounter, flow1)
        checkFlowAndSubscribedCounter(combinedCounter, flow2)

        combinedCounter += counter3
        checkFlowAndSubscribedCounter(combinedCounter, flow1)
        checkFlowAndSubscribedCounter(combinedCounter, flow2)
        checkFlowAndSubscribedCounter(combinedCounter, flow3)
    }


    private fun <T> Flow<T>.refCount(subscribedCounter: MutableStateFlow<Int>): Flow<T> =
        SubscriptionCountFlow(subscribedCounter, this)
}
