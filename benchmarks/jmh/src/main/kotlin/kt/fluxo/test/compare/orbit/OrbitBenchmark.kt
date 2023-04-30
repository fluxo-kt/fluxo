package kt.fluxo.test.compare.orbit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.simple.SimpleSyntax
import org.orbitmvi.orbit.syntax.simple.blockingIntent
import org.orbitmvi.orbit.syntax.simple.reduce

@Suppress("InjectDispatcher")
@OptIn(OrbitExperimental::class)
internal object OrbitBenchmark {
    fun mvvmpIntentStaticIncrement(): Int {
        val (host, job) = createHostAndJob()
        runBlocking {
            val intent: suspend SimpleSyntax<Int, Nothing>.() -> Unit = { reduce { state + 1 } }
            val launchDef = launchCommonBenchmarkWithStaticIntent(intent) { host.blockingIntent(transformer = it) }
            host.container.stateFlow.consumeCommonBenchmark(launchDef, job)
        }
        return host.container.stateFlow.value
    }

    fun mvvmpIntentAdd(value: Int = BENCHMARK_ARG): Int {
        val (host, job) = createHostAndJob()
        runBlocking {
            val launchDef = launchCommonBenchmark { host.blockingIntent { reduce { state + value } } }
            host.container.stateFlow.consumeCommonBenchmark(launchDef, job)
        }
        return host.container.stateFlow.value
    }

    private fun createHostAndJob(): Pair<ContainerHost<Int, Nothing>, Job> {
        val job = SupervisorJob()
        val dispatcher = Dispatchers.Unconfined
        val host = object : ContainerHost<Int, Nothing> {
            override val container = CoroutineScope(dispatcher + job).container<Int, Nothing>(0)
        }
        return host to job
    }
}
