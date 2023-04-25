package kt.fluxo.tests

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.SideEffectStrategy
import kt.fluxo.core.container
import kt.fluxo.core.debug.DEBUG
import kt.fluxo.core.intent.IntentStrategy.InBox.Direct
import kt.fluxo.core.intent.IntentStrategy.InBox.Lifo
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FluxoSettingsTest {
    @Test
    fun repeatOnSubscribedStopTimeout() {
        @Suppress("DEPRECATION")
        val container = container(Unit) {
            repeatOnSubscribedStopTimeout = 0L
            assertEquals(0L, repeatOnSubscribedStopTimeout)
        }
        container.close()
    }

    @Test
    fun sideEffectsStrategy() {
        val s = FluxoSettings.DEFAULT.copy()
        assertEquals(Channel.BUFFERED, s.sideEffectBufferSize)
        val share = SideEffectStrategy.SHARE()
        s.sideEffectStrategy = share
        assertEquals(share, s.sideEffectStrategy)
        assertEquals(0, s.sideEffectBufferSize)
    }

    @Test
    fun exceptionHandler() {
        val container = container(Unit) {
            closeOnExceptions = false
            exceptionHandler = null
            assertFalse(closeOnExceptions)

            closeOnExceptions = true
            exceptionHandler = null
            assertTrue(closeOnExceptions)

            onError {}
            assertFalse(closeOnExceptions)
        }
        container.close()
    }

    @Test
    fun debugChecks() {
        val container = container(Unit) {
            closeOnExceptions = false
            debugChecks = true
            assertTrue(closeOnExceptions)
        }
        container.close()
    }

    @Test
    fun settings_copy() {
        val s = FluxoSettings.DEFAULT.copy()

        assertTrue(s.optimized)
        assertNull(s.exceptionHandler)

        assertEquals(EmptyCoroutineContext, s.sideJobsContext)
        assertEquals(Dispatchers.Default, s.coroutineContext)
        assertNull(s.scope)

        assertEquals(SideEffectStrategy.RECEIVE, s.sideEffectStrategy)
        assertEquals(Direct, s.intentStrategy)
        assertNull(s.intentFilter)
        assertNull(s.bootstrapper)

        assertEquals(Channel.BUFFERED, s.sideEffectBufferSize)
        assertEquals(DEBUG, s.debugChecks)
        assertTrue(s.closeOnExceptions)
        assertTrue(s.lazy)
        assertNull(s.name)

        val share = SideEffectStrategy.SHARE()
        s.sideEffectStrategy = share
        s.intentStrategy = Lifo
        s.name = "test"

        assertEquals(share, s.sideEffectStrategy)
        assertEquals(0, s.sideEffectBufferSize)

        val c = s.copy()
        assertEquals(share, c.sideEffectStrategy)
        assertEquals(0, c.sideEffectBufferSize)
        assertEquals(Lifo, c.intentStrategy)
        assertEquals("test", c.name)
    }
}
