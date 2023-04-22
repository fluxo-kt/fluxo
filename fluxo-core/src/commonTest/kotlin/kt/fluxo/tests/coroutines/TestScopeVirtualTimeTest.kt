package kt.fluxo.tests.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kt.fluxo.test.IgnoreJs
import kt.fluxo.test.runUnitTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Test to be sure that [kotlinx.coroutines] work as expected.
 */
@IgnoreJs
class TestScopeVirtualTimeTest {

    private companion object {
        private const val USE_SAFE_WITH_TIMEOUT = true
        private const val USE_WITH_TIMEOUT = true
        private const val TIMEOUT_MS = 5_000L
        private const val TEST_REPETITIONS = 1_000
    }

    // TODO: Benchmark Mutex implementations
    //  - StateFlow-based seems to be 2-3x slower than Mutex or Semaphore
    //  - Semaphore seems to be 20-30% faster than Mutex!

    @Test
    fun with_timeout_unsafety() = runUnitTest {
        // https://github.com/Kotlin/kotlinx.coroutines/blob/dea2ca5/kotlinx-coroutines-test/README.md#using-withtimeout-inside-runtest
        // https://github.com/Kotlin/kotlinx.coroutines/issues/3588
        assertFailsWith<TimeoutCancellationException> {
            repeat(times = 10_000) {
                val mutex = Mutex(locked = true)
                val job = backgroundScope.launch(Dispatchers.Default) {
                    // Deliberately time-skipping delay here! (Dispatchers.Default used)
                    delay(10)
                    mutex.unlock()
                    // busy wait
                    @Suppress("ControlFlowWithEmptyBody", "EmptyWhileBlock")
                    while (currentCoroutineContext().isActive) {
                    }
                }
                // Deliberately unsafe withTimeout here!
                withTimeout(TIMEOUT_MS) {
                    mutex.withLock {}
                }
                job.cancelAndJoin()
            }
        }
    }


    @Test
    fun with_timeout_mutex() = testImplementation(
        createMutex = { Mutex(locked = true) },
        doUnlock = {
//            testLog("doUnlock (Mutex)")
            assertTrue(isLocked, "doUnlock called for non-locked Mutex (Mutex)")
            unlock()
        },
        waitForMutexUnlock = {
            if (isLocked) {
                withLock {
                    assertTrue(isLocked, "Mutex.isLocked = false when locked (Mutex)")
                }
            }
//            testLog("waitForMutexUnlock: unlocked (Mutex)")
            assertFalse(isLocked, "Mutex.isLocked = false (Mutex)")
        },
    )

    @Test
    fun with_timeout_semaphore() = testImplementation(
        createMutex = { Semaphore(permits = 1, acquiredPermits = 1) },
        doUnlock = {
//            testLog("doUnlock (Semaphore)")
            try {
                release()
            } catch (_: IllegalStateException) {
                fail("doUnlock called for non-locked Mutex (Semaphore)")
            }
        },
        waitForMutexUnlock = {
            // wait for unlock
            acquire()
            try {
//                testLog("waitForMutexUnlock: unlocked (Semaphore)")
                release()
            } catch (_: IllegalStateException) {
                fail("Mutex.isLocked = false (Semaphore)")
            }
        },
    )

    // MutableStateFlow as a locked "Mutex"
    @Test
    fun with_timeout_state_flow() = testImplementation(
        // MutableStateFlow as a locked "Mutex"
        createMutex = { MutableStateFlow(true) },
        doUnlock = {
//            testLog("doUnlock (StateFlow)")
            assertTrue(compareAndSet(expect = true, update = false), "doUnlock called for non-locked Mutex (StateFlow)")
            assertFalse(value, "couldn't unlock Mutex (Mutex)")
        },
        waitForMutexUnlock = {
            if (value) {
                // wait for unlock
                assertFalse(first { !it }, "Mutex waiting failed (StateFlow)")
            }
//            testLog("waitForMutexUnlock: unlocked (StateFlow)")
            assertFalse(value, "Mutex.isLocked = false (StateFlow)")
        },
    )

    private inline fun <M> testImplementation(
        crossinline createMutex: () -> M,
        crossinline doUnlock: M.() -> Unit,
        crossinline waitForMutexUnlock: suspend M.() -> Unit,
    ) = runTest {
        // Scopes that will do the actual work in the background
        val scopes = mapOf(
            "TestScope + Dispatchers.Default" to (this + Dispatchers.Default),
            "BackgroundTestScope + Dispatchers.Default" to (backgroundScope + Dispatchers.Default),
            "Job + Dispatchers.Default" to CoroutineScope(Job() + Dispatchers.Default),
        )
        repeat(TEST_REPETITIONS) { iteration ->
            for ((scopeName, scope) in scopes) {
                val info = "#$iteration, $scopeName"
                try {
//                    testLog("start: $info")
                    val mutex = createMutex()
                    val asyncJob = scope.launch {
                        mutex.doUnlock()
                        // busy wait
                        @Suppress("ControlFlowWithEmptyBody", "EmptyWhileBlock")
                        while (currentCoroutineContext().isActive) {
                        }
                    }

                    when {
                        USE_SAFE_WITH_TIMEOUT -> withContext(Dispatchers.Default) {
                            withTimeout(TIMEOUT_MS) {
                                mutex.waitForMutexUnlock()
                            }
                        }

                        USE_WITH_TIMEOUT -> withTimeout(TIMEOUT_MS) {
                            mutex.waitForMutexUnlock()
                        }

                        else -> mutex.waitForMutexUnlock()
                    }

                    asyncJob.cancelAndJoin()
//                    testLog("iteration finished\n")
                } catch (e: Throwable) {
                    throw IllegalStateException("$e // $info", e)
                }
            }
        }
    }
}
