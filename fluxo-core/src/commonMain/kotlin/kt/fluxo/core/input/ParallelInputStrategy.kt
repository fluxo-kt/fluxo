package kt.fluxo.core.input

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kt.fluxo.core.FluxoSettings
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmField

/**
 * Strategy with parallel processing of intents.
 * Can provide better responsiveness comparing to [Fifo][FifoInputStrategy].
 *
 * Use [CoroutineDispatcher.limitedParallelism] on [FluxoSettings.coroutineContext] to limit parallelism to some value.
 *
 * **IMPORTANT:** There is no guarantee that intents will be processed in any particular order!
 *
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 *
 * @see FifoInputStrategy
 * @see LifoInputStrategy
 * @see ChannelLifoInputStrategy
 */
internal class ParallelInputStrategy(
    private val start: CoroutineStart,
) : InputStrategy.Factory {

    companion object {
        @JvmField
        val DEFAULT: InputStrategy.Factory = ParallelInputStrategy(start = CoroutineStart.DEFAULT)

        @JvmField
        val DIRECT: InputStrategy.Factory = ParallelInputStrategy(start = CoroutineStart.UNDISPATCHED)
    }

    override fun toString() = "Parallel(start=$start)"

    override fun equals(other: Any?): Boolean {
        return this === other || other is ParallelInputStrategy && start == other.start
    }

    override fun hashCode(): Int = start.hashCode()


    override fun <Intent, State> invoke(scope: InputStrategyScope<Intent, State>): InputStrategy<Intent, State> = Parallel(scope, start)

    private class Parallel<in Intent, State>(
        handler: InputStrategyScope<Intent, State>,
        private val coroutineStart: CoroutineStart,
    ) : InputStrategy<Intent, State>(handler) {

        override val parallelProcessing: Boolean get() = true

        override fun queueIntent(intent: Intent): Job {
            return handler.launch(context = EmptyCoroutineContext, start = coroutineStart) {
                executeIntent(intent, null)
            }
        }
    }
}
