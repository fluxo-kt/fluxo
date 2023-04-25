package kt.fluxo.core.intent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.internal.Closeable
import kotlin.js.JsName
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.native.ObjCName

/**
 * Second version of [IntentStrategy] system.
 * Provides full control on how, when, and where to execute [Intent]s.
 *
 * @param isLaunchNeeded Enable to call [launch] for this strategy initialization
 * @param parallelProcessing Enable if strategy can process intents in parallel
 * @param rollbackOnCancellation Enable for state rollback on intent cancellation
 */
@ExperimentalFluxoApi
public abstract class IntentStrategy<in Intent, State>(
    @JvmField
    protected val handler: IntentStrategyScope<Intent, State>,
    @JvmField
    internal val isLaunchNeeded: Boolean,
    @JvmField
    internal val parallelProcessing: Boolean,

    private val rollbackOnCancellation: Boolean,
) : Closeable {

    internal constructor(handler: IntentStrategyScope<Intent, State>, isLaunchNeeded: Boolean, parallelProcessing: Boolean) : this(
        handler = handler,
        isLaunchNeeded = isLaunchNeeded,
        parallelProcessing = parallelProcessing,
        rollbackOnCancellation = !parallelProcessing,
    )


    /**
     * Launch the [Intent]s processing.
     *
     * **NOTE:** Can suspend until the [close] call!
     */
    public open suspend fun launch(): Unit = throw NotImplementedError(toString())


    /**
     * Queues the [Intent] for processing using this [IntentStrategy] in a suspendable way.
     */
    @JsName("queueIntentSuspend")
    public open suspend fun queueIntentSuspend(intent: Intent) {
        queueIntent(intent)
    }

    /**
     * Queues the [Intent] for processing using this [IntentStrategy], returning a [Job].
     */
    @JsName("queueIntent")
    public abstract fun queueIntent(intent: Intent): Job


    /**
     *
     * **NOTE:** Should always be called with [Job] that can be safely cancelled!
     *  [CoroutineScope] receiver here helps to reduce the possibility if misusage.
     */
    @JsName("executeIntent")
    @Suppress("UnusedReceiverParameter")
    protected suspend fun CoroutineScope.executeIntent(intent: Intent, deferred: CompletableDeferred<Unit>?) {
        // Don't waste resources launching cancelled intent
        if (deferred?.isCompleted == true) {
            return
        }
        val stateBeforeCancellation = handler.value
        try {
            handler.executeIntent(intent)
        } catch (ce: CancellationException) {
            // reset state as intent did not complete successfully
            if (rollbackOnCancellation) {
                handler.value = stateBeforeCancellation
            }
            deferred?.completeExceptionally(ce)
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            deferred?.completeExceptionally(e)
            handler.handleException(currentCoroutineContext(), e)
        } finally {
            deferred?.complete(Unit)
        }
    }

    override fun close() {}


    @ExperimentalFluxoApi
    public interface Factory {
        @JsName("create")
        public operator fun <Intent, State> invoke(scope: IntentStrategyScope<Intent, State>): IntentStrategy<Intent, State>
    }


    /**
     * Accessors for in-box intent strategies.
     */
    public companion object InBox {
        /**
         * `First-in, first-out` strategy. Provides strictly ordered processing: predictable and intuitive, the default choice.
         * Based on [Channel] to maintain the order of processing.
         *
         * **IMPORTANT:** The new intent will not start processing until the previous one is finished!
         *
         * Consider [Parallel] or [Lifo] instead if you need more responsiveness.
         *
         * @see Parallel
         * @see Lifo
         * @see ChannelLifo
         */
        @JsName("Fifo")
        @ObjCName("Fifo")
        @get:JvmName("Fifo")
        public val Fifo: Factory get() = FifoIntentStrategy

        /**
         * **Lifo**, `Last-in, first-out` strategy.
         * Optimized for a stream of events (e.g., user actions) in which new ones make previous ones obsolete.
         * Provides more responsiveness than [Fifo], but at the cost of possibly losing some intents!
         *
         * **IMPORTANT:** Cancels previous unfinished intent when receives a new one!
         *
         * **IMPORTANT:** There is no guarantee that the intents will not be processed in parallel!
         *
         * Consider [Parallel] if you need more responsiveness, but without dropping out any intents.
         *
         * See [ChannelLifo] for [Channel]-based implementation of the same strategy,
         * which allows to wait for the last intent to complete after canceling before
         * processing a new one, making the strategy strictly ordered.
         *
         * @see Parallel
         * @see ChannelLifo
         * @see Fifo
         */
        @JsName("Lifo")
        @ObjCName("Lifo")
        @get:JvmName("Lifo")
        public val Lifo: Factory get() = LifoIntentStrategy


        /**
         * Strategy with parallel processing of intents.
         * Can provide better responsiveness comparing to [Fifo].
         *
         * Use [CoroutineDispatcher.limitedParallelism] on [FluxoSettings.coroutineContext] to limit parallelism to some value.
         *
         * **IMPORTANT:** There is no guarantee that intents will be processed in any particular order!
         *
         * @see Fifo
         * @see Lifo
         * @see ChannelLifo
         */
        @JsName("Parallel")
        @ObjCName("Parallel")
        @get:JvmName("Parallel")
        public val Parallel: Factory get() = ParallelIntentStrategy.DEFAULT

        /**
         * [Parallel] strategy that will immediately execute intent until its first suspension point in the current thread
         * similarly to the coroutine being started using [Dispatchers.Unconfined].
         * However, when the coroutine is resumed from suspension it is dispatched
         * according to the [CoroutineDispatcher] in its context.
         *
         * @see CoroutineStart.UNDISPATCHED
         * @see Parallel
         */
        @ExperimentalFluxoApi
        @JsName("Direct")
        @ObjCName("Direct")
        @get:JvmName("Direct")
        public val Direct: Factory get() = ParallelIntentStrategy.DIRECT


        /**
         * [Channel]-based implementation for the **[Lifo]**, `Last-in, first-out` strategy.
         * Optimized for a stream of events (e.g., user actions) in which new ones make previous ones obsolete.
         * Provides more responsiveness than [Fifo], but at the cost of possibly losing some intents!
         *
         * **IMPORTANT:** Cancels previous unfinished intent when receives a new one!
         *
         * **IMPORTANT:** There is no guarantee that the intents will not be processed in parallel if [ordered] is `false`!
         *
         * Consider [Parallel] if you need more responsiveness, but without dropping out any intents.
         *
         * Consider simple non-[channel][Channel]-based [Lifo] if you don't need [strict order guarantee][ordered].
         *
         * @param ordered if `true`, the strategy will wait for the last intent to complete
         *  after canceling before processing a new one. This makes the strategy strictly ordered,
         *  without any moments of potentially parallel processing. As a consequence,
         *  it will allow the previous intent to block the next one, e.g. with busy-waiting.
         *
         * @see Parallel
         * @see Lifo
         * @see Fifo
         */
        @ExperimentalFluxoApi
        @Suppress("FunctionName")
        @JsName("ChannelLifo")
        @ObjCName("ChannelLifo")
        @JvmName("ChannelLifo")
        internal fun ChannelLifo(ordered: Boolean = true): Factory = ChannelLifoIntentStrategy(ordered)
    }
}
