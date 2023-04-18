package kt.fluxo.tests.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kt.fluxo.test.Platform
import kt.fluxo.test.runUnitTest
import kt.fluxo.test.testLog
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
        testLog("job_completion_with_children // childJob: $childJob")
        testLog("job_completion_with_children // childJob.isActive: ${childJob.isActive}")
        assertTrue(childJob.isActive, "childJob.isActive 1")
        testLog("job_completion_with_children // childJob.isCompleted: ${childJob.isCompleted}")
        assertFalse(childJob.isCompleted, "childJob.isCompleted 1")
        testLog("job_completion_with_children // childJob.isCancelled: ${childJob.isCancelled}")
        assertFalse(childJob.isCancelled, "childJob.isCancelled 1")
        job.children.toList().let {
            testLog("job_completion_with_children // job.children: $it")
            assertContentEquals(listOf(childJob), it, "job.children, check 1")
        }

        job.complete().let {
            testLog("job_completion_with_children // job.complete(): $it")
            assertTrue(it)
        }
        testLog("job_completion_with_children // job.isActive: ${job.isActive}")
        assertTrue(job.isActive, "job.isActive 2")
        testLog("job_completion_with_children // job.isCompleted: ${job.isCompleted}")
        assertFalse(job.isCompleted, "job.isCompleted 2")
        testLog("job_completion_with_children // job.isCancelled: ${job.isCancelled}")
        assertFalse(job.isCancelled, "job.isCancelled 2")
        job.children.toList().let {
            testLog("job_completion_with_children // job.children: $it")
            assertContentEquals(listOf(childJob), it, "job.children, check 2")
        }

        testLog("job_completion_with_children // childJob.isActive: ${childJob.isActive}")
        assertTrue(childJob.isActive, "childJob.isActive 3")
        testLog("job_completion_with_children // childJob.isActive: ${childJob.isCompleted}")
        assertFalse(childJob.isCompleted, "childJob.isCompleted 3")
        testLog("job_completion_with_children // childJob.isActive: ${childJob.isCancelled}")
        assertFalse(childJob.isCancelled, "childJob.isCancelled 3")

        var wasCalled = false
        val childJob2 = launch(job) { wasCalled = true }
        testLog("job_completion_with_children // childJob2.isActive: ${childJob2.isActive}")
        assertTrue(childJob2.isActive, "childJob2.isActive 4")
        testLog("job_completion_with_children // childJob2.isCompleted: ${childJob2.isCompleted}")
        assertFalse(childJob2.isCompleted, "childJob2.isCompleted 4")
        testLog("job_completion_with_children // childJob2.isCancelled: ${childJob2.isCancelled}")
        assertFalse(childJob2.isCancelled, "childJob2.isCancelled 4")
        job.children.toList().let {
            testLog("job_completion_with_children // job.children: $it")
            assertContentEquals(listOf(childJob, childJob2), it, "job.children, check 3")
        }

        testLog("childJob.cancel()")
        childJob.cancel()
        testLog("job_completion_with_children // childJob.isActive: ${childJob.isActive}")
        assertFalse(childJob.isActive, "childJob.isActive 5")
        testLog("job_completion_with_children // childJob.isActive: ${childJob.isCompleted}")
        assertFalse(childJob.isCompleted, "childJob.isCompleted 5")
        testLog("job_completion_with_children // childJob.isActive: ${childJob.isCancelled}")
        assertTrue(childJob.isCancelled, "childJob.isCancelled 5")

        testLog("job_completion_with_children // job.isActive: ${job.isActive}")
        assertTrue(job.isActive, "job.isActive 6")
        testLog("job_completion_with_children // job.isCompleted: ${job.isCompleted}")
        assertFalse(job.isCompleted, "job.isCompleted 6")
        testLog("job_completion_with_children // job.isCancelled: ${job.isCancelled}")
        assertFalse(job.isCancelled, "job.isCancelled 6")
        job.children.toList().let {
            testLog("job_completion_with_children // job.children: $it")
            assertContentEquals(listOf(childJob, childJob2), it, "job.children, check 4")
        }

        testLog("job.join()")
        job.join()
        testLog("job_completion_with_children // childJob.isActive: ${childJob.isActive}")
        assertFalse(childJob.isActive, "childJob.isActive 7")
        testLog("job_completion_with_children // childJob.isCompleted: ${childJob.isCompleted}")
        assertTrue(childJob.isCompleted, "childJob.isCompleted 7")
        testLog("job_completion_with_children // childJob.isCancelled: ${childJob.isCancelled}")
        assertTrue(childJob.isCancelled, "childJob.isCancelled 7")

        assertTrue(wasCalled, "Child job launched while in Completing state expected to be executed")
        testLog("job_completion_with_children // childJob2.isActive: ${childJob2.isActive}")
        assertFalse(childJob2.isActive, "childJob2.isActive 8")
        testLog("job_completion_with_children // childJob2.isCompleted: ${childJob2.isCompleted}")
        assertTrue(childJob2.isCompleted, "childJob2.isCompleted 8")
        testLog("job_completion_with_children // childJob2.isCancelled: ${childJob2.isCancelled}")
        assertFalse(childJob2.isCancelled, "childJob2.isCancelled 8")

        testLog("job_completion_with_children // job.isActive: ${job.isActive}")
        assertFalse(job.isActive, "job.isActive 9")
        testLog("job_completion_with_children // job.isCompleted: ${job.isCompleted}")
        assertTrue(job.isCompleted, "job.isCompleted 9")
        testLog("job_completion_with_children // job.isCancelled: ${job.isCancelled}")
        assertFalse(job.isCancelled, "job.isCancelled 9")
        job.children.toList().let {
            testLog("job_completion_with_children // job.children: $it")
            assertContentEquals(listOf(), it, "job.children, check 5")
        }

        job.cancel()
        assertFalse(job.isCancelled, "job.isCancelled 10")
    }

    @Test
    fun job_cancellation_with_children() = runUnitTest {
        val job = Job()
        val childJob = CoroutineScope(job).launch(Dispatchers.Default) {
            // NOTE: delay is safe bacause of Dispatcher
            delay(Long.MAX_VALUE)
        }
        testLog("job_cancellation_with_children // childJob: $childJob")
        testLog("job_cancellation_with_children // childJob.isActive: ${childJob.isActive}")
        assertTrue(childJob.isActive, "childJob.isActive 1")
        testLog("job_cancellation_with_children // childJob.isCompleted: ${childJob.isCompleted}")
        assertFalse(childJob.isCompleted, "childJob.isCompleted 1")
        testLog("job_cancellation_with_children // childJob.isCancelled: ${childJob.isCancelled}")
        assertFalse(childJob.isCancelled, "childJob.isCancelled 1")
        job.children.toList().let {
            testLog("job_cancellation_with_children // job.children: $it")
            assertContentEquals(listOf(childJob), it, "job.children, check 1")
        }

        job.cancel()
        testLog("job_cancellation_with_children // job.isActive: ${job.isActive}")
        assertFalse(job.isActive, "job.isActive 2")
        testLog("job_cancellation_with_children // job.isCompleted: ${job.isCompleted}")
        assertFalse(job.isCompleted, "job.isCompleted 2")
        testLog("job_cancellation_with_children // job.isCancelled: ${job.isCancelled}")
        assertTrue(job.isCancelled, "job.isCancelled 2")

        testLog("job_cancellation_with_children // childJob.isActive: ${childJob.isActive}")
        assertFalse(childJob.isActive, "childJob.isActive 3")
        testLog("job_cancellation_with_children // childJob.isCompleted: ${childJob.isCompleted}")
        assertFalse(childJob.isCompleted, "childJob.isCompleted 3")
        testLog("job_cancellation_with_children // childJob.isCancelled: ${childJob.isCancelled}")
        assertTrue(childJob.isCancelled, "childJob.isCancelled 3")

        val childJob2 = launch(job) {
            fail("Shouldn't be called!")
        }
        testLog("job_cancellation_with_children // childJob2.isActive: ${childJob2.isActive}")
        assertFalse(childJob2.isActive, "childJob2.isActive 4")
        testLog("job_cancellation_with_children // childJob2.isCompleted: ${childJob2.isCompleted}")
        assertFalse(childJob2.isCompleted, "childJob2.isCompleted 4")
        testLog("job_cancellation_with_children // childJob2.isCancelled: ${childJob2.isCancelled}")
        assertTrue(childJob2.isCancelled, "childJob2.isCancelled 4")
        val childrenList = job.children.toList()
        job.children.toList().let {
            testLog("job_cancellation_with_children // job.children: $it")
            if (!Platform.isNative || childrenList.isNotEmpty()) {
                assertContentEquals(listOf(childJob), childrenList, "job.children, check 2")
            }
        }

        childJob.join()
        testLog("job_cancellation_with_children // childJob.isActive: ${childJob.isActive}")
        assertFalse(childJob.isActive, "childJob.isActive 5")
        testLog("job_cancellation_with_children // childJob.isCompleted: ${childJob.isCompleted}")
        assertTrue(childJob.isCompleted, "childJob.isCompleted 5")
        testLog("job_cancellation_with_children // childJob.isCancelled: ${childJob.isCancelled}")
        assertTrue(childJob.isCancelled, "childJob.isCancelled 5")

        testLog("job_cancellation_with_children // childJob2.isActive: ${childJob2.isActive}")
        assertFalse(childJob2.isActive, "childJob2.isActive 6")
        testLog("job_cancellation_with_children // childJob2.isCompleted: ${childJob2.isCompleted}")
        assertTrue(childJob2.isCompleted || Platform.isNative, "childJob2.isCompleted 6")
        testLog("job_cancellation_with_children // childJob2.isCancelled: ${childJob2.isCancelled}")
        assertTrue(childJob2.isCancelled, "childJob2.isCancelled 6")

        testLog("job_cancellation_with_children // job.isActive: ${job.isActive}")
        assertFalse(job.isActive, "job.isActive 7")
        testLog("job_cancellation_with_children // job.isCompleted: ${job.isCompleted}")
        assertTrue(job.isCompleted, "job.isCompleted 7")
        testLog("job_cancellation_with_children // job.isCancelled: ${job.isCancelled}")
        assertTrue(job.isCancelled, "job.isCancelled 7")
        job.children.toList().let {
            testLog("job_cancellation_with_children // job.children: $it")
            assertContentEquals(listOf(), it, "job.children, check 3")
        }
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
