package kt.fluxo.test.compare

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal object SameThreadScheduledExecutorService : ScheduledExecutorService {

    override fun submit(task: Runnable): Future<*> {
        task.run()
        return CompletableFuture.completedFuture(Unit)
    }

    override fun execute(command: Runnable) = throw UnsupportedOperationException("Not implemented")

    override fun shutdown() = throw UnsupportedOperationException("Not implemented")

    override fun shutdownNow() = throw UnsupportedOperationException("Not implemented")

    override fun isShutdown() = throw UnsupportedOperationException("Not implemented")

    override fun isTerminated() = throw UnsupportedOperationException("Not implemented")

    override fun awaitTermination(timeout: Long, unit: TimeUnit) = throw UnsupportedOperationException("Not implemented")

    override fun <T : Any?> submit(task: Callable<T>) = throw UnsupportedOperationException("Not implemented")

    override fun <T : Any?> submit(task: Runnable, result: T) = throw UnsupportedOperationException("Not implemented")

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>) =
        throw UnsupportedOperationException("Not implemented")

    override fun <T : Any?> invokeAll(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit,
    ) = throw UnsupportedOperationException("Not implemented")

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>) =
        throw UnsupportedOperationException("Not implemented")

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit) =
        throw UnsupportedOperationException("Not implemented")

    override fun schedule(command: Runnable, delay: Long, unit: TimeUnit) =
        throw UnsupportedOperationException("Not implemented")

    override fun <V : Any?> schedule(callable: Callable<V>, delay: Long, unit: TimeUnit) =
        throw UnsupportedOperationException("Not implemented")

    override fun scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit) =
        throw UnsupportedOperationException("Not implemented")

    override fun scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit) =
        throw UnsupportedOperationException("Not implemented")
}
