package kt.fluxo.test

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ScopedBlockingWorkSimulator(private val scope: CoroutineScope) {

    private val job = atomic<Job?>(null)

    init {
        scope.produce<Unit>(Dispatchers.Default) {
            awaitClose {
                job.value?.cancel()
            }
        }
    }

    @Suppress("ControlFlowWithEmptyBody", "EmptyWhileBlock")
    fun simulateWork() {
        job.updateAndGet {
            if (it != null) {
                error("Can be invoked only once")
            }
            scope.launch(Dispatchers.Default) {
                while (currentCoroutineContext().isActive) {
                }
            }
        }.let {
            runBlocking(it!!) { it.join() }
        }
    }
}
