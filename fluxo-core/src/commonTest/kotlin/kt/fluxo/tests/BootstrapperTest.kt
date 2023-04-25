package kt.fluxo.tests

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.repeatOnSubscription
import kt.fluxo.core.updateState
import kt.fluxo.test.CoroutineScopeAwareTest
import kt.fluxo.test.IgnoreJs
import kt.fluxo.test.IgnoreJvm
import kt.fluxo.test.runBlocking
import kt.fluxo.test.runUnitTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(InternalCoroutinesApi::class)
internal class BootstrapperTest : CoroutineScopeAwareTest() {

    @Test
    @IgnoreJs
    fun b_without_noOp_can_close_store_run_blocking() = runBlocking {
        var isInitialized = false
        val store = scope.container(INIT) {
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
    fun b_is_working_properly() = runUnitTest {
        var isInitialized = false
        val store = container(INIT) {
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
    fun b_with_eager_store() = runUnitTest {
        var isInitialized = false
        val mutex = Mutex(locked = true)
        val store = scope.container(INIT) {
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

    // TODO: After waiting for 5s, the test coroutine is not completing
    //  :jvmTest mac
    //  :testReleaseUnitTest mac
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4797515958/jobs/8534599655#step:8:506
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4797515958/jobs/8534599655#step:8:1030
    @Test
    fun b_intent() = runUnitTest {
        var hadIntent = false
        val store = scope.container(INIT) {
            debugChecks = true
            @Suppress("DEPRECATION")
            onCreate {
                send {
                    hadIntent = true
                }.join()
            }
        }
        assertNotNull(store.start(), "Expected Job for explicit lazy start").join()
        assertTrue(hadIntent, "bootstrapper should call intent successfully")
    }

    @Test
    fun b_update_state() = runUnitTest {
        val newValue = "$INIT.update"
        val store = scope.container(INIT) {
            debugChecks = true
            bootstrapper = {
                assertEquals(INIT, value)
                updateState { "$it.update" }
                assertEquals(newValue, value)
            }
        }
        assertNotNull(store.start(), "Expected Job for explicit lazy start").join()
        assertEquals(newValue, store.value)
    }

    // TODO: fails under Win and macOS occasionally, should be fixed!
    // kt.fluxo.BootstrapperTest.b_repeat_on_subscription FAILED
    //    kotlinx.coroutines.test.UncompletedCoroutinesError at null:-1
    // :mingwX64BackgroundTest
    // :testDebugUnitTest
    //
    // https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4018550114/jobs/6904304918#step:8:266
    // MacOS :jvmTest
    // Win :testReleaseUnitTest
    //   TimeoutCancellationException: Timed out waiting for 2000 ms
    @Test
    fun b_side_effect() = runUnitTest {
        val store = scope.container<String, String>(INIT) {
            debugChecks = true
            bootstrapper = {
                postSideEffect("se1")
            }
        }
        assertEquals("se1", store.sideEffectFlow.first())
    }

    // FIXME: UncompletedCoroutinesError: After waiting for 5s, the test coroutine is not completing
    //  jvm
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4756275550/jobs/8451589421#step:8:404
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4759202632/jobs/8458210085#step:8:548
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4600266159/jobs/8126636920#step:8:397
    @Test
    @IgnoreJvm
    fun b_side_job() = runUnitTest {
        val store = backgroundScope.container<String, String>(INIT) {
            debugChecks = true
            bootstrapperJob { wasRestarted ->
                assertEquals(INIT, value)
                assertFalse(wasRestarted)
                emit { updateState { "$it.sideJob" } }
            }
        }
        assertEquals("$INIT.sideJob", store.first { it != INIT })
        store.closeAndWait()
    }

    // TODO: Failed on win with "After waiting for 5s, the test coroutine is not completing"
    // jvm, android
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4600266159/jobs/8126636920#step:8:397
    @Test
    @Ignore // TODO: Should be returned after `fluxo-event-stream` will be added
    fun b_cancellation() = runUnitTest {
//        val def = CompletableDeferred<FluxoEvent<*, *, *>>()
//        scope.container(INIT) {
//            debugChecks = false
//            bootstrapper = {
//                cancel()
//            }
//            onEvent {
//                if (it is FluxoEvent.BootstrapperCancelled) {
//                    def.complete(it)
//                }
//            }
//        }.start()
//        assertIs<FluxoEvent.BootstrapperCancelled<*, *, *>>(def.await())
    }

    // TODO: TimeoutCancellationException: Timed out waiting for 2000 ms
    //  :jvm
    @Test
    fun b_repeat_on_subscription_with_side_effects() = runUnitTest {
        val store = backgroundScope.container<String, String>(INIT) {
            onStart {
                var i = 0
                repeatOnSubscription(stopTimeout = 0) { wasRestarted ->
                    assertFalse(wasRestarted)
                    updateState { "update${i++}" }
                }
            }
        }
        assertContentEquals(listOf(INIT, "update0"), store.take(2).toList())
        assertContentEquals(listOf("update0", "update1"), store.take(2).toList())
        store.closeAndWait()
    }

    // TODO: UncompletedCoroutinesError: After waiting for 2000 ms, the test coroutine is not completing
    //  jvm, android
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4018550114/jobs/6904304918#step:8:299
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4018550114/jobs/6904304918#step:8:506
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4018550114/jobs/6904304918#step:8:565
    @Test
    fun b_repeat_on_subscription_no_side_effects() = runUnitTest {
        val store = container(INIT) {
            onStart {
                var i = 0
                repeatOnSubscription(stopTimeout = 0) { wasRestarted ->
                    assertFalse(wasRestarted)
                    updateState { "update${i++}" }
                }
            }
        }
        assertContentEquals(listOf(INIT, "update0"), store.take(2).toList())
        assertContentEquals(listOf("update0", "update1"), store.take(2).toList())
        store.closeAndWait()
    }
}
