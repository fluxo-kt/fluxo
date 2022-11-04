package kt.fluxo

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kt.fluxo.core.container
import kt.fluxo.test.IgnoreJs
import kt.fluxo.test.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
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
    fun bootstrapper_is_working_properly() = runTest {
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
}
