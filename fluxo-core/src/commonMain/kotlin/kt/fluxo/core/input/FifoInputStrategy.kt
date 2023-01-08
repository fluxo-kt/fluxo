package kt.fluxo.core.input

/**
 * **Fifo**, `First-in, first-out` strategy. Provides strictly ordered processing: predictable and intuitive, the default choice.
 * Based on [Channel][ChannelBasedInputStrategy] to maintain the order of processing.
 *
 * **IMPORTANT:** The new intent will not start processing until the previous one is finished!
 *
 * Consider [Parallel][ParallelInputStrategy] or [Lifo][LifoInputStrategy] instead if you need more responsiveness.
 *
 * @see ParallelInputStrategy
 * @see LifoInputStrategy
 * @see ChannelLifoInputStrategy
 */
internal object FifoInputStrategy : InputStrategy.Factory {

    override fun toString() = "Fifo"

    override fun <Intent, State> invoke(scope: InputStrategyScope<Intent, State>): InputStrategy<Intent, State> =
        ChannelBasedInputStrategy(scope, resendUndelivered = true)
}
