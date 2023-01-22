package kt.fluxo.tests.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kt.fluxo.test.Platform
import kt.fluxo.test.runUnitTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Test to be sure that [kotlinx.coroutines] work as expected.
 */
class JobTest {

    @Test
    fun job_completion() = runUnitTest {
        val job = Job()
        assertTrue(job.isActive)
        assertFalse(job.isCompleted)
        assertFalse(job.isCancelled)
        assertContentEquals(listOf(), job.children.toList())

        assertTrue(job.complete())
        assertFalse(job.isActive)
        assertTrue(job.isCompleted)
        assertFalse(job.isCancelled)

        val childJob = launch(job) {
            fail("Shouldn't be called!")
        }
        assertFalse(childJob.isActive)
        assertFalse(childJob.isCompleted)
        assertTrue(childJob.isCancelled)
        assertContentEquals(listOf(), job.children.toList())

        childJob.join()
        assertFalse(childJob.isActive)
        assertTrue(childJob.isCompleted)
        assertTrue(childJob.isCancelled)
    }

    @Test
    fun job_cancellation() = runUnitTest {
        val job = Job()
        job.cancel()
        assertFalse(job.isActive)
        assertTrue(job.isCompleted)
        assertTrue(job.isCancelled)

        val childJob = launch(job) {
            fail("Shouldn't be called!")
        }
        assertFalse(childJob.isActive)
        assertFalse(childJob.isCompleted)
        assertTrue(childJob.isCancelled)
        assertContentEquals(listOf(), job.children.toList())

        childJob.join()
        assertTrue(childJob.isCompleted)
        assertTrue(childJob.isCancelled)
    }

    @Test
    fun job_completion_with_children() = runUnitTest {
        val job = Job()
        val childJob = launch(Dispatchers.Default + job) {
            // NOTE: delay is safe bacause of Dispatcher
            delay(Long.MAX_VALUE)
        }
        assertTrue(childJob.isActive)
        assertFalse(childJob.isCompleted)
        assertFalse(childJob.isCancelled)
        assertContentEquals(listOf(childJob), job.children.toList())

        assertTrue(job.complete())
        assertTrue(job.isActive)
        assertFalse(job.isCompleted)
        assertFalse(job.isCancelled)
        assertContentEquals(listOf(childJob), job.children.toList())

        assertTrue(childJob.isActive)
        assertFalse(childJob.isCompleted)
        assertFalse(childJob.isCancelled)

        var wasCalled = false
        val childJob2 = launch(job) { wasCalled = true }
        assertTrue(childJob2.isActive)
        assertFalse(childJob2.isCompleted)
        assertFalse(childJob2.isCancelled)
        assertContentEquals(listOf(childJob, childJob2), job.children.toList())

        childJob.cancel()
        assertFalse(childJob.isActive)
        assertFalse(childJob.isCompleted)
        assertTrue(childJob.isCancelled)

        assertTrue(job.isActive)
        assertFalse(job.isCompleted)
        assertFalse(job.isCancelled)
        assertContentEquals(listOf(childJob, childJob2), job.children.toList())

        job.join()
        assertFalse(childJob.isActive)
        assertTrue(childJob.isCompleted)
        assertTrue(childJob.isCancelled)

        assertTrue(wasCalled, "Child job launched while in Completing state expected to be executed")
        assertFalse(childJob2.isActive)
        assertTrue(childJob2.isCompleted)
        assertFalse(childJob2.isCancelled)

        assertFalse(job.isActive)
        assertTrue(job.isCompleted)
        assertFalse(job.isCancelled)
        assertContentEquals(listOf(), job.children.toList())

        job.cancel()
        assertFalse(job.isCancelled)
    }

    @Test
    fun job_cancellation_with_children() = runUnitTest {
        val job = Job()
        val childJob = CoroutineScope(job).launch(Dispatchers.Default) {
            // NOTE: delay is safe bacause of Dispatcher
            delay(Long.MAX_VALUE)
        }
        assertTrue(childJob.isActive)
        assertFalse(childJob.isCompleted)
        assertFalse(childJob.isCancelled)
        assertContentEquals(listOf(childJob), job.children.toList())

        job.cancel()
        assertFalse(job.isActive)
        assertFalse(job.isCompleted)
        assertTrue(job.isCancelled)

        assertFalse(childJob.isActive)
        assertFalse(childJob.isCompleted)
        assertTrue(childJob.isCancelled)

        val childJob2 = launch(job) {
            fail("Shouldn't be called!")
        }
        assertFalse(childJob2.isActive)
        assertFalse(childJob2.isCompleted)
        assertTrue(childJob2.isCancelled)
        val childrenList = job.children.toList()
        if (!Platform.isNative || childrenList.isNotEmpty()) {
            assertContentEquals(listOf(childJob), childrenList)
        }

        childJob.join()
        assertFalse(childJob.isActive)
        assertTrue(childJob.isCompleted)
        assertTrue(childJob.isCancelled)

        assertFalse(childJob2.isActive)
        assertTrue(childJob2.isCompleted || Platform.isNative)
        assertTrue(childJob2.isCancelled)

        assertFalse(job.isActive)
        assertTrue(job.isCompleted)
        assertTrue(job.isCancelled)
        assertContentEquals(listOf(), job.children.toList())
    }

    @Test
    fun parent_job_does_not_cancel_on_child_cancellation() = runUnitTest {
        // TestScope fails test with error on TestScope cancellation.
        // backgroundScope Job can be cancelled without problems.

        val bgJob = backgroundScope.coroutineContext[Job]
        Job(bgJob).cancel()
        bgJob?.cancel()

        Job(coroutineContext[Job]).cancel()
    }
}
