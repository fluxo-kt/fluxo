package kt.fluxo

import app.cash.turbine.test
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kt.fluxo.core.container
import kt.fluxo.core.dsl.SideJobScope.RestartState
import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.dsl.accept
import kt.fluxo.core.intent
import kt.fluxo.core.intercept.FluxoEvent
import kt.fluxo.core.store
import kt.fluxo.test.unitTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class SideJobTest {

    @Test
    fun sj_on_mvi_intent() = unitTest {
        val store = backgroundScope.store("a", handler = { state ->
            when (state) {
                "a" -> sideJob {
                    assertEquals(RestartState.Initial, restartState)
                    assertEquals("a", currentStateWhenStarted)
                    updateState { if (it == "a") "b" else it }
                    postIntent("b")
                }

                "b" -> sideJob {
                    assertEquals(RestartState.Restarted, restartState)
                    assertEquals("b", currentStateWhenStarted)
                    updateState { if (it == "b") "c" else it }
                }
            }
        })

        store.stateFlow.test {
            assertEquals("a", awaitItem())

            @Suppress("DEPRECATION")
            store.accept("a")

            assertEquals("b", awaitItem())
            assertEquals("c", awaitItem())
        }
    }

    @Test
    fun sj_on_mvvm_intent() = unitTest {
        val store = backgroundScope.container("a")

        fun secondIntent() = store.intent {
            sideJob {
                assertEquals(RestartState.Restarted, restartState)
                assertEquals("b", currentStateWhenStarted)
                updateState { "c" }
            }
        }

        store.send {
            sideJob {
                assertEquals(RestartState.Initial, restartState)
                assertEquals("a", currentStateWhenStarted)
                updateState { "b" }
                secondIntent()
            }
        }
        store.stateFlow.test {
            assertEquals("a", awaitItem())
            assertEquals("b", awaitItem())
            assertEquals("c", awaitItem())
        }
    }

    @Test
    fun sj_side_effect() = unitTest {
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
    fun sj_error() = unitTest {
        var caught: Throwable? = null
        val store = kt.fluxo.core.container<String, String>("init") {
            exceptionHandler { _, e ->
                caught = e
            }
        }
        store.intent {
            sideJob {
                throw UnsupportedOperationException()
            }
        }
        store.eventsFlow.test {
            while (true) {
                val event = awaitItem()
                if (event is FluxoEvent.SideJobError) {
                    assertEquals(RestartState.Initial, event.restartState)
                    assertEquals(StoreScope.DEFAULT_SIDE_JOB, event.key)
                    assertIs<UnsupportedOperationException>(event.e)
                    cancelAndIgnoreRemainingEvents()
                    break
                }
            }
        }
        assertIs<UnsupportedOperationException>(caught)
        assertTrue(store.isActive)
        store.close()
    }

    @Test
    fun sj_restart_state() = unitTest {
        val store = container<String, String>("init")
        store.intent {
            sideJob {
                assertEquals(RestartState.Initial, restartState)
                delay(timeMillis = 1_000)
                updateState { "a" }
            }
            sideJob {
                assertEquals(RestartState.Restarted, restartState)
                updateState { "b" }
            }
        }
        store.stateFlow.test {
            assertEquals("init", awaitItem())
            assertEquals("b", awaitItem())
        }
        store.close()
    }
}
