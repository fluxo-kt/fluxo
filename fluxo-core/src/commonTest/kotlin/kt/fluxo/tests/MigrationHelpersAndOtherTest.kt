package kt.fluxo.tests

import kt.fluxo.core.container
import kotlin.test.Test

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
        val container = container(Unit) {
            @Suppress("DEPRECATION")
            repeatOnSubscribedStopTimeout = 0L
        }
        container.close()
    }
}
