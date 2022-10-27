package kt.fluxo.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kt.fluxo.core.dsl.InputStrategyScope
import kt.fluxo.core.intercept.StoreRequest
import kt.fluxo.core.strategy.FifoInputStrategy
import kt.fluxo.core.strategy.LifoInputStrategy
import kt.fluxo.core.strategy.ParallelInputStrategy
import kotlin.jvm.JvmStatic

public abstract class InputStrategy<Intent, State> {

    public open fun createQueue(): Channel<StoreRequest<Intent, State>> {
        return Channel(capacity = Channel.UNLIMITED, onBufferOverflow = BufferOverflow.SUSPEND)
    }

    public open val parallelProcessing: Boolean get() = false

    public open val rollbackOnCancellation: Boolean get() = !parallelProcessing

    public abstract suspend fun InputStrategyScope<Intent, State>.processRequests(filteredQueue: Flow<StoreRequest<Intent, State>>)


    /**
     * Accessors for in-box input strategies.
     */
    @Suppress("FunctionName", "UNCHECKED_CAST", "MemberVisibilityCanBePrivate")
    public companion object {
        /**
         * Ordered processing strategy. Predictable and intuitive. Best for background.
         * Consider [Lifo] for the UI or more responsiveness instead.
         */
        public fun <Intent, State> Fifo(): InputStrategy<Intent, State> = FifoInputStrategy as InputStrategy<Intent, State>

        /**
         * UI events oriented strategy. Cancels previous unfinished intents when receives new one.
         *
         * Provides more responsiveness, but can lose some intents!
         */
        public fun <Intent, State> Lifo(): InputStrategy<Intent, State> = LifoInputStrategy as InputStrategy<Intent, State>

        /** Parallel processing of all intents. No guarantee that inputs will be processed in any given order. */
        public fun <Intent, State> Parallel(): InputStrategy<Intent, State> = ParallelInputStrategy as InputStrategy<Intent, State>
    }
}
