package kt.fluxo.tests.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        assertTrue(childJob.isActive, "childJob.isActive 1")
        assertFalse(childJob.isCompleted, "childJob.isCompleted 1")
        assertFalse(childJob.isCancelled, "childJob.isCancelled 1")
        job.children.toList().let {
            assertContentEquals(listOf(childJob), it, "job.children, check 1: $it")
        }

        assertTrue(job.complete(), "job.complete() 2")
        assertTrue(job.isActive, "job.isActive 2")
        assertFalse(job.isCompleted, "job.isCompleted 2")
        assertFalse(job.isCancelled, "job.isCancelled 2")
        job.children.toList().let {
            assertContentEquals(listOf(childJob), it, "job.children, check 2: $it")
        }

        assertTrue(childJob.isActive, "childJob.isActive 3")
        assertFalse(childJob.isCompleted, "childJob.isCompleted 3")
        assertFalse(childJob.isCancelled, "childJob.isCancelled 3")

        var wasCalled = false
        val childJob2 = launch(job) { wasCalled = true }
        assertTrue(childJob2.isActive, "childJob2.isActive 4")
        assertFalse(childJob2.isCompleted, "childJob2.isCompleted 4")
        assertFalse(childJob2.isCancelled, "childJob2.isCancelled 4")
        job.children.toList().let {
            assertContentEquals(listOf(childJob, childJob2), it, "job.children, check 3: $it")
        }

        childJob.cancel()
        assertFalse(childJob.isActive, "childJob.isActive 5")
        // childJob.isCompleted can have any value at this moment (jvm, linuxX64, iosX64, macosX64, tvosX64)
        assertTrue(childJob.isCancelled, "childJob.isCancelled 5")

        assertTrue(job.isActive, "job.isActive 6")
        assertFalse(job.isCompleted, "job.isCompleted 6")
        assertFalse(job.isCancelled, "job.isCancelled 6")
        job.children.toList().let {
            assertContentEquals(listOf(childJob, childJob2), it, "job.children, check 4: $it")
        }

        job.join()
        assertFalse(childJob.isActive, "childJob.isActive 7")
        assertTrue(childJob.isCompleted, "childJob.isCompleted 7")
        assertTrue(childJob.isCancelled, "childJob.isCancelled 7")

        assertTrue(wasCalled, "Child job launched while in Completing state expected to be executed")
        assertFalse(childJob2.isActive, "childJob2.isActive 8")
        assertTrue(childJob2.isCompleted, "childJob2.isCompleted 8")
        assertFalse(childJob2.isCancelled, "childJob2.isCancelled 8")

        assertFalse(job.isActive, "job.isActive 9")
        assertTrue(job.isCompleted, "job.isCompleted 9")
        assertFalse(job.isCancelled, "job.isCancelled 9")
        job.children.toList().let {
            assertContentEquals(listOf(), it, "job.children, check 5: $it")
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
        assertTrue(childJob.isActive, "childJob.isActive 1")
        assertFalse(childJob.isCompleted, "childJob.isCompleted 1")
        assertFalse(childJob.isCancelled, "childJob.isCancelled 1")
        job.children.toList().let {
            assertContentEquals(listOf(childJob), it, "job.children, check 1: $it")
        }

        job.cancel()
        assertFalse(job.isActive, "job.isActive 2")
        // job.isCompleted can have any value at this moment (jvm, linuxX64, mingwX64, macosX64)
        assertTrue(job.isCancelled, "job.isCancelled 2")

        assertFalse(childJob.isActive, "childJob.isActive 3")
        // childJob.isCompleted can have any value at this moment (linuxX64, tvosX64, watchosX64)
        assertTrue(childJob.isCancelled, "childJob.isCancelled 3")

        val childJob2 = launch(job) {
            fail("Shouldn't be called!")
        }
        assertFalse(childJob2.isActive, "childJob2.isActive 4")
        assertFalse(childJob2.isCompleted, "childJob2.isCompleted 4")
        assertTrue(childJob2.isCancelled, "childJob2.isCancelled 4")
        job.children.toList().let {
            if (it.isNotEmpty()) { // mingwX64, sometimes for JVM/Android too!
                assertContentEquals(listOf(childJob), it, "job.children, check 2: $it")
            }
        }

        childJob.join()
        assertFalse(childJob.isActive, "childJob.isActive 5")
        assertTrue(childJob.isCompleted, "childJob.isCompleted 5")
        assertTrue(childJob.isCancelled, "childJob.isCancelled 5")

        assertFalse(childJob2.isActive, "childJob2.isActive 6")
        // childJob2.isCompleted can have any value at this moment (Android, mingwX64)
        assertTrue(childJob2.isCancelled, "childJob2.isCancelled 6")

        assertFalse(job.isActive, "job.isActive 7")
        assertTrue(job.isCompleted, "job.isCompleted 7")
        assertTrue(job.isCancelled, "job.isCancelled 7")
        job.children.toList().let {
            assertContentEquals(listOf(), it, "job.children, check 3: $it")
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
