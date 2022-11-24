package kt.fluxo.tests.data

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kt.fluxo.core.data.GuaranteedEffect
import kt.fluxo.core.store
import kt.fluxo.test.CoroutineScopeAwareTest
import kotlin.test.Test
import kotlin.test.assertEquals


internal class GuaranteedEffectTest : CoroutineScopeAwareTest() {

    @Test
    fun guaranteed_effect_can_resend_itself(): TestResult {
        val store = scope.store<Unit, Int, GuaranteedEffect<*>>(initialState = 0, handler = {
            postSideEffect(GuaranteedEffect(it))
        }) {
            debugChecks = true
        }
        return runTest {
            var effects = 0
            store.send(Unit)

            store.sideEffectFlow.takeWhile {
                !it.handleOrResend { effects++ != 0 }
            }.collect()

            assertEquals(2, effects)
        }
    }
}
