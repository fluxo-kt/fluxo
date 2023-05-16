package kt.fluxo.tests.dsl

import app.cash.turbine.test
import kt.fluxo.core.container
import kt.fluxo.core.dsl.accept
import kt.fluxo.test.runUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("DEPRECATION", "DEPRECATION_ERROR")
class StoreScopeTest {
    @Test
    fun scope__noOp() = runUnitTest {
        val container = backgroundScope.container(0) { debugChecks = false }
        container.stateFlow.test {
            assertEquals(0, awaitItem())
            container.accept {
                noOp()
            }.join()
        }
    }

    @Test
    fun scope__launch() = runUnitTest {
        val container = backgroundScope.container(0) { debugChecks = false }
        container.stateFlow.test {
            assertEquals(0, awaitItem())
            container.accept {
                assertFailsWith<NotImplementedError> { launch {} }
                assertFailsWith<NotImplementedError> { async {} }
            }.join()
        }
    }
}
