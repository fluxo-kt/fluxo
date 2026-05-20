package kt.fluxo.tests

import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.dsl.accept
import kt.fluxo.core.intent
import kt.fluxo.core.store
import kt.fluxo.core.updateState
import kt.fluxo.test.runUnitTest
import kotlin.coroutines.EmptyCoroutineContext
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
    fun sj_on_mvvmp_intent() = runUnitTest {
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
    fun side_job_overloads_delegate_to_full_implementation() = runUnitTest {
        suspend fun assertRuns(block: suspend StoreScope<Nothing, Int, Int>.() -> Unit) {
            val store = backgroundScope.container<Int, Int>(0)
            try {
                store.test {
                    assertEquals(0, awaitItem())
                    store.send { block(this) }
                    assertEquals(1, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            } finally {
                store.closeAndWait()
            }
        }

        assertRuns { sideJob { updateState { it + 1 } } }
        assertRuns { sideJob("key") { updateState { it + 1 } } }
        assertRuns { sideJob(EmptyCoroutineContext) { updateState { it + 1 } } }
        assertRuns { sideJob(CoroutineStart.DEFAULT) { updateState { it + 1 } } }
        assertRuns { sideJob(onError = null) { updateState { it + 1 } } }
        assertRuns { sideJob("key", EmptyCoroutineContext) { updateState { it + 1 } } }
        assertRuns { sideJob("key", CoroutineStart.DEFAULT) { updateState { it + 1 } } }
        assertRuns { sideJob("key", onError = null) { updateState { it + 1 } } }
        assertRuns { sideJob(EmptyCoroutineContext, CoroutineStart.DEFAULT) { updateState { it + 1 } } }
        assertRuns { sideJob(EmptyCoroutineContext, onError = null) { updateState { it + 1 } } }
        assertRuns { sideJob(CoroutineStart.DEFAULT, onError = null) { updateState { it + 1 } } }
        assertRuns { sideJob("key", EmptyCoroutineContext, CoroutineStart.DEFAULT) { updateState { it + 1 } } }
        assertRuns { sideJob("key", EmptyCoroutineContext, onError = null) { updateState { it + 1 } } }
        assertRuns { sideJob("key", CoroutineStart.DEFAULT, onError = null) { updateState { it + 1 } } }
        assertRuns { sideJob(EmptyCoroutineContext, CoroutineStart.DEFAULT, onError = null) { updateState { it + 1 } } }
        assertRuns {
            sideJob(CoroutineName("side-job-test")) {
                assertEquals("side-job-test", currentCoroutineContext()[CoroutineName]?.name)
                updateState { it + 1 }
            }
        }
        val caught = CompletableDeferred<Throwable>()
        val store = backgroundScope.container<Int, Int>(0)
        try {
            store.test {
                assertEquals(0, awaitItem())
                store.send {
                    sideJob("same-key") {
                        updateState { 1 }
                        CompletableDeferred<Unit>().await()
                    }
                }
                assertEquals(1, awaitItem())
                store.send {
                    sideJob("same-key") { wasRestarted ->
                        updateState { if (wasRestarted) 2 else 100 }
                    }
                }
                assertEquals(2, awaitItem())
                store.send {
                    sideJob(onError = caught::complete) {
                        throw IllegalStateException("side job failed")
                    }
                }
                assertEquals("side job failed", caught.await().message)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            store.closeAndWait()
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
                    // NOTE: delay is safe because of Dispatcher
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
