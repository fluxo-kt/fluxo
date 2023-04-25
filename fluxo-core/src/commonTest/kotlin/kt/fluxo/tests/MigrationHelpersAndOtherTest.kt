package kt.fluxo.tests

import kt.fluxo.core.container
import kotlin.test.Test

class MigrationHelpersAndOtherTest {
    @Test
    fun intentStrategyLifo() {
        val container = container(Unit) {
            intentStrategy = Lifo
        }
        container.close()
    }
}
