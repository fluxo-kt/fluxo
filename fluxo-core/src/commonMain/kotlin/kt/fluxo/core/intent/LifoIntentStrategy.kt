package kt.fluxo.core.intent

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

/**
 * **Lifo**, `Last-in, first-out` strategy.
 * Optimized for a stream of events (e.g., user actions) in which new ones make previous ones obsolete.
 * Provides more responsiveness than [Fifo][FifoIntentStrategy], but at the cost of possibly losing some intents!
 *
 * **IMPORTANT:** Cancels previous unfinished intent when receives a new one!
 *
 * **IMPORTANT:** There is no guarantee that the intents will not be processed in parallel!
 *
 * Consider [Parallel][ParallelIntentStrategy] if you need more responsiveness, but without dropping out any intents.
 *
 * See [ChannelLifo][ChannelLifoIntentStrategy] for [Channel]-based implementation of the same strategy,
 * which allows to wait for the last intent to complete after canceling before processing a new one,
 * making the strategy strictly ordered.
 *
 * @see ParallelIntentStrategy
 * @see ChannelLifoIntentStrategy
 * @see FifoIntentStrategy
 */
internal object LifoIntentStrategy : IntentStrategy.Factory {

    override fun toString() = "Lifo"

    override fun <Intent, State> invoke(scope: IntentStrategyScope<Intent, State>): IntentStrategy<Intent, State> = Lifo(scope)

    private class Lifo<in Intent, State>(handler: IntentStrategyScope<Intent, State>) : IntentStrategy<Intent, State>(handler) {

        /**
         * Next intent can start without waiting for the end of previous one, so mark strategy as parallel.
         */
        override val parallelProcessing: Boolean get() = true

        private val previousJob = atomic<Job?>(null)

        override fun queueIntent(intent: Intent): Job {
            var old = previousJob.value
            old?.cancel()
            val newJob = handler.launch(context = EmptyCoroutineContext, start = CoroutineStart.DEFAULT) {
                executeIntent(intent, null)
            }
            while (true) {
                if (previousJob.compareAndSet(old, newJob)) {
                    return newJob
                }
                old = previousJob.value
                old?.cancel()
            }
        }

        override fun close() {
            previousJob.value?.cancel()
            previousJob.value = null
        }
    }
}
