package kt.fluxo.tests

import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.test.runUnitTest
import kotlin.test.Test

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
}
