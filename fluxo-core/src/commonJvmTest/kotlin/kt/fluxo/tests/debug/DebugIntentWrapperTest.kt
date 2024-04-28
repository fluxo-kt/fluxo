@file:Suppress("FunctionNaming")

package kt.fluxo.tests.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.IntentHandler
import kt.fluxo.core.container
import kt.fluxo.core.debug.DEBUG
import kt.fluxo.core.factory.FluxoStoreFactory
import kt.fluxo.core.factory.StoreDecorator
import kt.fluxo.core.factory.StoreDecoratorBase
import kt.fluxo.core.factory.StoreFactory
import kt.fluxo.core.updateState
import kt.fluxo.test.runUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DebugIntentWrapperTest {

    private companion object {
        private const val DEFAULT_INTENT_STRING =
            "kt.fluxo.core.dsl.StoreScope<kotlin.Nothing, kotlin.Int, kotlin.Nothing>.() -> kotlin.Unit"
    }

    @Test
    fun check_correct_name_for_lambda_intent() = runUnitTest {
        val factory = CatchIntentStoreFactory()
        val container = backgroundScope.container(0, factory = factory) { debugChecks = true }
        container.send { value = 1 }.join()
        val expected = when {
            DEBUG -> "check_correct_name_for_lambda_intent"
            else -> DEFAULT_INTENT_STRING
        }
        assertEquals(expected, factory.lastIntent.value?.toString())
    }

    @Test
    fun check_correct_name_for_lambda_intent_with_arguments() = runUnitTest {
        val factory = CatchIntentStoreFactory()
        val container = backgroundScope.container(0, factory = factory) { debugChecks = true }
        val time = System.currentTimeMillis().toInt()
        val q = 2
        container.send { value = time + q }.join()

        val expected = when {
            DEBUG -> "check_correct_name_for_lambda_intent_with_arguments(time=$time, q=2)"
            else -> DEFAULT_INTENT_STRING
        }
        assertEquals(expected, factory.lastIntent.value?.toString())
    }

    @Test
    fun check_correct_name_for_vm_intent() = runUnitTest {
        val factory = CatchIntentStoreFactory()
        val vm = AddModel(factory)
        vm.add(1).join()
        val expected = when {
            DEBUG -> "add(number=1)"
            else -> DEFAULT_INTENT_STRING
        }
        assertEquals(expected, factory.lastIntent.value?.toString())
    }

    private class AddModel(factory: StoreFactory? = null) {

        private val container = container(initialState = 0, factory = factory) { debugChecks = true }

        fun add(number: Int) = container.send {
            updateState {
                it + number
            }
        }
    }


    private class CatchIntentStoreFactory(
        private val delegate: StoreFactory = FluxoStoreFactory,
    ) : StoreFactory() {

        val lastIntent = MutableStateFlow<Any?>(null)

        override fun <Intent, State, SideEffect : Any> createForDecoration(
            initialState: State,
            handler: IntentHandler<Intent, State, SideEffect>,
            settings: FluxoSettings<Intent, State, SideEffect>,
        ): StoreDecorator<Intent, State, SideEffect> {
            return CatchIntentStoreDecorator(lastIntent, delegate.createForDecoration(initialState, handler, settings))
        }

        private class CatchIntentStoreDecorator<Intent, State, SideEffect : Any>(
            private val lastIntent: MutableStateFlow<Any?>,
            store: StoreDecorator<Intent, State, SideEffect>,
        ) : StoreDecoratorBase<Intent, State, SideEffect>(store) {

            override suspend fun onIntent(intent: Intent) {
                lastIntent.value = intent
                super.onIntent(intent)
            }
        }
    }
}
