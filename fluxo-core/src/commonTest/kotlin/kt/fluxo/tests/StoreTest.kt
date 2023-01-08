package kt.fluxo.tests

import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.test.runUnitTest
import kt.fluxo.tests.InputStrategyTest.Companion.ALL_STRATEGIES
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreTest {
    @Test
    fun lazy_container_cancellation() = runUnitTest {
        this.container(0) { lazy = true }.closeAndWait()
        backgroundScope.container(0) { lazy = true }.closeAndWait()
        // should be auto closed by test
        backgroundScope.container(0) { lazy = true }
    }

    @Test
    fun eager_container_cancellation() = runUnitTest {
        this.container(0) { lazy = false }.closeAndWait()
        backgroundScope.container(0) { lazy = false }.closeAndWait()
        // should be auto closed by test
        backgroundScope.container(0) { lazy = false }
    }

    @Test
    fun started_container_cancellation() = runUnitTest {
        this.container(0) { lazy = true }.apply { start().join() }.closeAndWait()
        backgroundScope.container(0) { lazy = true }.apply { start().join() }.closeAndWait()
        this.container(0) { lazy = false }.apply { start().join() }.closeAndWait()
        backgroundScope.container(0) { lazy = false }.apply { start().join() }.closeAndWait()
        // should be auto closed by test
        backgroundScope.container(0) { lazy = true }.start().join()
        backgroundScope.container(0) { lazy = false }.start().join()
    }

    @Test
    fun intent_processed_container_cancellation() = runUnitTest {
        for (strategy in ALL_STRATEGIES) {
            for (container in arrayOf(
                container(0) { inputStrategy = strategy },
                backgroundScope.container(0) { inputStrategy = strategy },
            )) {
                assertEquals(0, container.value)
                container.send { value = 1 }.join()
                assertEquals(1, container.value)
                container.closeAndWait()
            }
        }
    }
}
