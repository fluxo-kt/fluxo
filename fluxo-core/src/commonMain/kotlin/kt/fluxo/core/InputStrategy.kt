package kt.fluxo.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kt.fluxo.core.dsl.InputStrategyScope
import kt.fluxo.core.strategy.FifoInputStrategy
import kt.fluxo.core.strategy.LifoInputStrategy
import kt.fluxo.core.strategy.ParallelInputStrategy
import kotlin.jvm.JvmName

public abstract class InputStrategy {

    public open fun <Request> createQueue(): Channel<Request> {
        return Channel(capacity = Channel.UNLIMITED, onBufferOverflow = BufferOverflow.SUSPEND)
    }

    public open val parallelProcessing: Boolean get() = false

    public open val rollbackOnCancellation: Boolean get() = !parallelProcessing

    public abstract suspend fun <Request> InputStrategyScope<Request>.processRequests(queue: Flow<Request>)


    /**
     * Accessors for in-box input strategies.
     */
    public companion object InBox {
        /**
         * Ordered processing strategy. Predictable and intuitive. Best for background.
         * Consider [Lifo] for the UI or more responsiveness instead.
         */
        @get:JvmName("Fifo")
        public val Fifo: InputStrategy get() = FifoInputStrategy

        /**
         * UI events oriented strategy. Cancels previous unfinished intents when receives new one.
         *
         * Provides more responsiveness, but can lose some intents!
         */
        @get:JvmName("Lifo")
        public val Lifo: InputStrategy get() = LifoInputStrategy

        /** Parallel processing of all intents. No guarantee that inputs will be processed in any given order. */
        @get:JvmName("Parallel")
        public val Parallel: InputStrategy get() = ParallelInputStrategy
    }
}
