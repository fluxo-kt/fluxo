package kt.fluxo.tests

import kt.fluxo.core.container
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MigrationHelpersAndOtherTest {
    @Test
    fun inputStrategyLifo() {
        val container = container(Unit) {
            inputStrategy = Lifo
        }
        container.close()
    }

    @Test
    fun settingsRepeatOnSubscribedStopTimeout() {
        val container = container(Unit) @Suppress("DEPRECATION") {
            repeatOnSubscribedStopTimeout = 0L
            assertEquals(0L, repeatOnSubscribedStopTimeout)
        }
        container.close()
    }

    @Test
    fun settingsExceptionHandler() {
        val container = container(Unit) {
            closeOnExceptions = false
            exceptionHandler = null
            assertTrue(closeOnExceptions)

            onError {}
            assertFalse(closeOnExceptions)
        }
        container.close()
    }

    @Test
    fun settingsDebugChecks() {
        val container = container(Unit) {
            closeOnExceptions = false
            debugChecks = true
            assertTrue(closeOnExceptions)
        }
        container.close()
    }
}
