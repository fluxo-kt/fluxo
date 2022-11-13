package kt.fluxo

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import kt.fluxo.core.container
import kt.fluxo.core.dsl.SideJobScope.RestartState
import kt.fluxo.core.intercept.FluxoEvent
import kt.fluxo.core.repeatOnSubscription
import kt.fluxo.test.IgnoreJs
import kt.fluxo.test.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(InternalCoroutinesApi::class)
internal class BootstrapperTest {

    private val scope = CoroutineScope(Job() + CoroutineExceptionHandler { _, _ -> /*just be silent*/ })

    @AfterTest
    fun afterTest() {
        scope.cancel()
    }

    @Test
    @IgnoreJs
    fun bootstrapper_without_noOp_can_close_store_run_blocking() = runBlocking {
        var isInitialized = false
        val store = scope.container("init") {
            lazy = true
            debugChecks = true
            bootstrapper = {
                isInitialized = true
            }
        }
        val job = assertNotNull(store.start(), "Expected Job for explicit lazy start")
        job.join()
        assertNotNull(job.getCancellationException(), "Should be cancelled by Guardian")
        assertTrue(isInitialized, "bootstrapper should complete normally")
        assertFalse(store.isActive, "store should be closed")
    }

    @Test
    fun bootstrapper_is_working_properly() = runTest(dispatchTimeoutMs = 2000) {
        var isInitialized = false
        val store = container("init") {
            debugChecks = true
            bootstrapper = {
                isInitialized = true
                noOp()
            }
        }
        assertNotNull(store.start(), "Expected Job for explicit lazy start").join()
        assertTrue(isInitialized, "bootstrapper should complete normally")
        assertTrue(store.isActive, "store should be active")
        store.close()
        assertFalse(store.isActive, "store should be active")
    }

    @Test
    fun bootstrapper_with_eager_store() = runTest(dispatchTimeoutMs = 2000) {
        var isInitialized = false
        val mutex = Mutex(locked = true)
        val store = scope.container("init") {
            lazy = false
            onStart {
                noOp()
                isInitialized = true
                mutex.unlock()
            }
        }
        mutex.lock()
        assertTrue(isInitialized, "bootstrapper should complete normally")
        assertTrue(store.isActive, "store should be active")
    }

    @Test
    fun bootstrapper_intent() = runTest(dispatchTimeoutMs = 2000) {
        var hadIntent = false
        val store = scope.container("init") {
            debugChecks = true
            @Suppress("DEPRECATION")
            onCreate {
                postIntent {
                    hadIntent = true
                }.join()
            }
        }
        assertNotNull(store.start(), "Expected Job for explicit lazy start").join()
        assertTrue(hadIntent, "bootstrapper should call intent successfully")
    }

    @Test
    fun bootstrapper_update_state() = runTest(dispatchTimeoutMs = 2000) {
        val store = scope.container("init") {
            debugChecks = true
            bootstrapper = {
                updateState { "$it.update" }
            }
        }
        assertNotNull(store.start(), "Expected Job for explicit lazy start").join()
        assertEquals("init.update", store.state)
    }

    @Test
    fun bootstrapper_side_effect() = runTest(dispatchTimeoutMs = 2000) {
        val store = scope.container<String, String>("init") {
            debugChecks = true
            bootstrapper = {
                postSideEffect("se1")
            }
        }
        assertEquals("se1", store.sideEffectFlow.first())
    }

    @Test
    fun bootstrapper_side_job() = runTest(dispatchTimeoutMs = 2000) {
        val store = scope.container<String, String>("init") {
            debugChecks = true
            bootstrapper = {
                sideJob {
                    assertEquals("init", currentStateWhenStarted)
                    assertEquals(RestartState.Initial, restartState)
                    postIntent {
                        updateState { "$it.sideJob" }
                    }
                }
            }
        }
        assertEquals("init.sideJob", store.stateFlow.first { it != "init" })
    }

    @Test
    fun bootstrapper_cancellation() = runTest(dispatchTimeoutMs = 2000) {
        val def = CompletableDeferred<FluxoEvent<*, *, *>>()
        scope.container("init") {
            debugChecks = false
            bootstrapper = {
                cancel()
            }
            onEvent {
                if (it is FluxoEvent.BootstrapperCancelled) {
                    def.complete(it)
                }
            }
        }.start()
        assertIs<FluxoEvent.BootstrapperCancelled<*, *, *>>(def.await())
    }

    @Test
    fun bootstrapper_repeat_on_subscription() = runTest(dispatchTimeoutMs = 2000) {
        val store = scope.container<String, String>("init") {
            onStart {
                var i = 0
                repeatOnSubscription(stopTimeout = 0) {
                    updateState { "update${i++}" }
                }
            }
        }
        assertContentEquals(listOf("init", "update0"), store.stateFlow.take(2).toList())
        assertContentEquals(listOf("update0", "update1"), store.stateFlow.take(2).toList())
    }
}
