package kt.fluxo.tests.data

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kt.fluxo.core.SideEffectStrategy
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.data.GuaranteedEffect
import kt.fluxo.core.store
import kt.fluxo.test.CoroutineScopeAwareTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class GuaranteedEffectTest : CoroutineScopeAwareTest() {

    @Test
    fun guaranteed_effect_can_resend_itself() = runTest {
        val store = scope.store<Unit, Int, GuaranteedEffect<Unit>>(initialState = 0, handler = {
            postSideEffect(GuaranteedEffect(it))
        }) {
            debugChecks = true
        }

        var effects = 0
        store.send(Unit)

        store.sideEffectFlow.takeWhile {
            assertIs<Unit>(it.rawContent)
            val handled = it.handleOrResend { effects++ != 0 }
            if (handled) {
                assertFalse(it.handleOrResend { true })
                assertNull(it.content)
                assertFailsWith<IllegalStateException> {
                    it.resend()
                }
            }
            !handled
        }.collect()

        assertEquals(2, effects)
        store.closeAndWait()
    }

    @Test
    fun guaranteed_effect_closing() = runTest {
        val store = store<Int, Int, GuaranteedEffect<Int>>(initialState = 0, handler = {
            postSideEffect(GuaranteedEffect(it))
        }) {
            lazy = false
            debugChecks = false
            sideEffectStrategy = SideEffectStrategy.SHARE(replay = 1)
        }
        yield()
        repeat(3, store::send)
        yield()
        assertEquals(2, assertNotNull(store.sideEffectFlow.first().content))
        store.closeAndWait()
    }
}
