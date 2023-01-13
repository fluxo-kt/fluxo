package kt.fluxo.tests.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kt.fluxo.core.internal.getAndAdd
import kt.fluxo.core.internal.toCancellationException
import kt.fluxo.test.runUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class UtilTest {

    @Test
    fun to_cancellation_exception() {
        val ce = CancellationException("")
        assertSame(ce, ce.toCancellationException())

        val e = Throwable()
        val ece = e.toCancellationException()
        assertIs<CancellationException>(ece)
        assertSame(e, ece.cause)
        assertEquals("Exception: $e", ece.message)

        assertNull(null.toCancellationException())
    }


    @Test
    fun mutable_state_flow__get_and_add__simple() {
        val flow = MutableStateFlow(0)
        val times = 100
        repeat(times) {
            assertEquals(it, flow.value)
            flow.getAndAdd(1)
        }
        assertEquals(times, flow.value)
    }

    @Test
    fun mutable_state_flow__get_and_add__concurrent() = runUnitTest {
        val flow = MutableStateFlow(0)
        val times = 75
        val scopes = arrayOf(this, backgroundScope, backgroundScope + Dispatchers.Default)
        val jobs = (0 until times).flatMap {
            scopes.map {
                it.launch {
                    repeat(times) {
                        flow.getAndAdd(1)
                    }
                }
            }
        }
        jobs.joinAll()
        assertEquals(times * times * scopes.size, flow.value)
    }
}
