package kt.fluxo.core.intent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kt.fluxo.core.FluxoSettings
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmField

/**
 * Strategy with parallel processing of intents.
 * Can provide better responsiveness comparing to [Fifo][FifoIntentStrategy].
 *
 * Use [CoroutineDispatcher.limitedParallelism] on [FluxoSettings.coroutineContext] to limit parallelism to some value.
 *
 * **IMPORTANT:** There is no guarantee that intents will be processed in any particular order!
 *
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 *
 * @see FifoIntentStrategy
 * @see LifoIntentStrategy
 * @see ChannelLifoIntentStrategy
 */
internal class ParallelIntentStrategy(
    private val start: CoroutineStart,
) : IntentStrategy.Factory {

    companion object {
        @JvmField
        val DEFAULT: IntentStrategy.Factory = ParallelIntentStrategy(start = CoroutineStart.DEFAULT)

        @JvmField
        val DIRECT: IntentStrategy.Factory = ParallelIntentStrategy(start = CoroutineStart.UNDISPATCHED)
    }

    override fun toString() = "Parallel(start=$start)"

    override fun equals(other: Any?): Boolean {
        return this === other || other is ParallelIntentStrategy && start == other.start
    }

    override fun hashCode(): Int = start.hashCode()


    override fun <Intent, State> invoke(scope: IntentStrategyScope<Intent, State>): IntentStrategy<Intent, State> = Parallel(scope, start)

    private class Parallel<in Intent, State>(
        handler: IntentStrategyScope<Intent, State>,
        private val coroutineStart: CoroutineStart,
    ) : IntentStrategy<Intent, State>(
        handler = handler,
        isLaunchNeeded = false,
        parallelProcessing = true,
    ) {
        override suspend fun queueIntentSuspend(intent: Intent) {
            // TODO: Separate DIRECT implementation from Parallel to remove the ambiguity
            if (coroutineStart === CoroutineStart.UNDISPATCHED) {
                // Optimize suspend calls to DIRECT strategy, remove the new coroutine creation and launch.
                handler.executeIntent(intent, null)
            } else {
                queueIntent(intent)
            }
        }

        override fun queueIntent(intent: Intent): Job {
            return handler.launch(context = EmptyCoroutineContext, start = coroutineStart) {
                executeIntent(intent, null)
            }
        }
    }
}
