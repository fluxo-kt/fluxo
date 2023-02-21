package kt.fluxo.tests

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.dsl.accept
import kt.fluxo.core.intent
import kt.fluxo.core.store
import kt.fluxo.core.updateState
import kt.fluxo.test.runUnitTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class SideJobTest {

    @Test
    fun sj_on_mvi_intent() = runUnitTest {
        val store = backgroundScope.store("a", handler = { state ->
            when (state) {
                "a" -> sideJob { wasRestarted ->
                    assertEquals("a", value)
                    assertFalse(wasRestarted)
                    updateState { if (it == "a") "b" else it }
                    emit("b")
                }

                "b" -> sideJob { wasRestarted ->
                    assertEquals("b", value)
                    assertTrue(wasRestarted)
                    updateState { if (it == "b") "c" else it }
                }
            }
        })

        store.test {
            assertEquals("a", awaitItem())

            @Suppress("DEPRECATION")
            store.accept("a")

            assertEquals("b", awaitItem())
            assertEquals("c", awaitItem())
        }
    }

    @Test
    fun sj_on_mvvm_intent() = runUnitTest {
        val store = backgroundScope.container("a")

        fun secondIntent() = store.intent {
            sideJob { wasRestarted ->
                assertEquals("b", value)
                assertTrue(wasRestarted)
                updateState { "c" }
            }
        }

        store.send {
            sideJob { wasRestarted ->
                assertEquals("a", value)
                assertFalse(wasRestarted)
                updateState { "b" }
                secondIntent()
            }
        }
        store.test {
            assertEquals("a", awaitItem())
            assertEquals("b", awaitItem())
            assertEquals("c", awaitItem())
        }
    }

    @Test
    fun sj_side_effect() = runUnitTest {
        for (scope in arrayOf(backgroundScope, this)) {
            val store = scope.container<String, String>("init")
            store.intent {
                sideJob {
                    postSideEffect("a")
                    postSideEffect("b")
                }
            }
            store.sideEffectFlow.test {
                assertEquals("a", awaitItem())
                assertEquals("b", awaitItem())
                store.close()
                awaitComplete()
            }
            assertFalse(store.isActive)
            assertTrue(scope.isActive)
        }
    }

    @Test
    @Ignore // TODO: Should be returned after `fluxo-event-stream` will be added
    fun sj_error() = runUnitTest {
        var caught: Throwable? = null
        val store = backgroundScope.container<String, String>("init") {
            onError { caught = it }
        }
        store.intent {
            sideJob {
                throw UnsupportedOperationException()
            }
        }
        // FIXME:
//        store.eventsFlow.test {
//            while (true) {
//                val event = awaitItem()
//                if (event is FluxoEvent.SideJobError) {
//                    assertFalse(event.wasRestarted)
//                    assertEquals(DEFAULT_SIDE_JOB, event.key)
//                    assertIs<UnsupportedOperationException>(event.e)
//                    cancelAndIgnoreRemainingEvents()
//                    break
//                }
//            }
//        }
        assertIs<UnsupportedOperationException>(caught)
        assertTrue(store.isActive, "Store is closed. Expected to be active")
        store.closeAndWait()
    }

    @Test
    fun sj_restart_state() = runUnitTest {
        val store = container<String, String>("init")
        store.intent {
            sideJob { wasRestarted ->
                assertFalse(wasRestarted)
                withContext(Dispatchers.Default) {
                    delay(timeMillis = 1_000)
                }
                updateState { "a" }
            }
            sideJob { wasRestarted ->
                assertTrue(wasRestarted)
                updateState { "b" }
            }
        }
        store.test {
            assertEquals("init", awaitItem())
            assertEquals("b", awaitItem())
        }
        store.close()
    }
}
