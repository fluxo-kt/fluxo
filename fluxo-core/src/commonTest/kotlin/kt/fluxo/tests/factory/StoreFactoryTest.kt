package kt.fluxo.tests.factory

import kt.fluxo.core.FluxoIntentS
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.SideEffectsStrategy
import kt.fluxo.core.container
import kt.fluxo.core.debug.DEBUG
import kt.fluxo.core.debug.DebugStoreDecorator
import kt.fluxo.core.factory.FluxoStoreFactory
import kt.fluxo.core.internal.FluxoIntentHandler
import kt.fluxo.core.internal.FluxoStore
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class StoreFactoryTest {
    @Test
    fun debug_decorator() {
        val container = container<Int, Int>(0) {
            debugChecks = true
        }
        if (DEBUG) {
            assertIs<DebugStoreDecorator<*, Int, Int>>(container)
        } else {
            assertIs<FluxoStore<*, Int, Int>>(container)
        }
        container.close()


        val container2 = container(0) {
            debugChecks = true
        }
        if (DEBUG) {
            assertIs<DebugStoreDecorator<*, Int, Nothing>>(container2)
        } else {
            assertIs<FluxoStore<*, Int, Nothing>>(container2)
        }
        container2.close()
    }

    @Test
    fun create_with_no_side_effects() {
        val handler = FluxoIntentHandler<Int, Nothing>()
        val settings = FluxoSettings<FluxoIntentS<Int>, Int, Nothing>().apply {
            debugChecks = true
            sideEffectsStrategy = SideEffectsStrategy.CONSUME
        }
        val error = assertFailsWith<IllegalArgumentException> {
            FluxoStoreFactory.create(initialState = 0, handler = handler, settings = settings)
        }
        assertContains(error.message!!, "Expected SideEffectsStrategy.DISABLE")
    }

    @Test
    fun default_params() {
        FluxoStoreFactory.create(0, FluxoIntentHandler()).close()
    }
}
