package kt.fluxo.core.intent

/**
 * **Fifo**, `First-in, first-out` strategy. Provides strictly ordered processing: predictable and intuitive, the default choice.
 * Based on [Channel][ChannelBasedIntentStrategy] to maintain the order of processing.
 *
 * **IMPORTANT:** The new intent will not start processing until the previous one is finished!
 *
 * Consider [Parallel][ParallelIntentStrategy] or [Lifo][LifoIntentStrategy] instead if you need more responsiveness.
 *
 * @see ParallelIntentStrategy
 * @see LifoIntentStrategy
 * @see ChannelLifoIntentStrategy
 */
internal object FifoIntentStrategy : IntentStrategy.Factory {

    override fun toString() = "Fifo"

    override fun <Intent, State> invoke(scope: IntentStrategyScope<Intent, State>): IntentStrategy<Intent, State> =
        ChannelBasedIntentStrategy(scope, resendUndelivered = true)
}
