@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.intent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.cancelConsumed
import kotlinx.coroutines.launch
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.internal.toCancellationException
import kotlin.coroutines.EmptyCoroutineContext

/**
 * [Channel]-based implementation for the **Lifo**, `Last-in, first-out` strategy.
 * Optimized for a stream of events (e.g., user actions) in which new ones make previous ones obsolete.
 * Provides more responsiveness than [Fifo][FifoIntentStrategy], but at the cost of possibly losing some intents!
 *
 * **IMPORTANT:** Cancels previous unfinished intent when receives a new one!
 *
 * **IMPORTANT:** There is no guarantee that the intents will not be processed in parallel if [ordered] is `false`!
 *
 * Consider [Parallel][ParallelIntentStrategy] if you need more responsiveness, but without dropping out any intents.
 *
 * Consider simple non-[channel][Channel]-based [Lifo][LifoIntentStrategy]
 * if you don't need [strict order guarantee][ordered].
 *
 * @param ordered if `true`, the strategy will wait for the last intent to complete
 *  after canceling before processing a new one. This makes the strategy strictly ordered,
 *  without any moments of potentially parallel processing. As a consequence,
 *  it will allow the previous intent to block the next one, e.g. with busy-waiting.
 *
 * @see ParallelIntentStrategy
 * @see LifoIntentStrategy
 * @see FifoIntentStrategy
 */
@ExperimentalFluxoApi
internal class ChannelLifoIntentStrategy(
    private val ordered: Boolean,
) : IntentStrategy.Factory {

    override fun toString() = "ChannelLifo(ordered=$ordered)"

    override fun equals(other: Any?): Boolean {
        return this === other || other is ChannelLifoIntentStrategy && ordered == other.ordered
    }

    override fun hashCode(): Int = ordered.hashCode()


    override fun <Intent, State> invoke(scope: IntentStrategyScope<Intent, State>): IntentStrategy<Intent, State> =
        ChannelLifo(scope, ordered)

    @Suppress("JS_FAKE_NAME_CLASH")
    private class ChannelLifo<Intent, State>(
        scope: IntentStrategyScope<Intent, State>,
        private val ordered: Boolean,
    ) : ChannelBasedIntentStrategy<Intent, State>(
        handler = scope,
        resendUndelivered = false,
        /** Signal if the next intent can start without waiting for the end of the earlier one, making the strategy parallel. */
        parallelProcessing = !ordered,
    ) {
        override fun <Request> createQueue(onUndeliveredElement: ((Request) -> Unit)?): Channel<Request> {
            return Channel(
                capacity = Channel.CONFLATED,
                /** In fact, it will work as [BufferOverflow.DROP_OLDEST] because of [Channel.CONFLATED] */
                onBufferOverflow = BufferOverflow.SUSPEND,
                onUndeliveredElement = onUndeliveredElement,
            )
        }

        /**
         * Reads all elements from the [requestsChannel], calls [executeIntent] for each [Intent] via [emit] method.
         * [cancels][Channel.cancel] (consumes) the channel afterwards.
         *
         * The crucial difference is that when the [requestsChannel] emits a new value
         * then the action block for the previous value is cancelled!
         *
         * It is a custom combination of [kotlinx.coroutines.flow.emitAll]
         * and [kotlinx.coroutines.flow.collectLatest]
         * aka [kotlinx.coroutines.flow.internal.ChannelFlowTransformLatest.flowCollect].
         *
         * @see kotlinx.coroutines.channels.consume
         * @see kotlinx.coroutines.flow.consumeAsFlow
         * @see kotlinx.coroutines.flow.collectLatest
         * @see kotlinx.coroutines.flow.mapLatest
         * @see kotlinx.coroutines.flow.transformLatest
         * @see kotlinx.coroutines.flow.internal.ChannelFlowTransformLatest.flowCollect
         * @see kotlinx.coroutines.flow.emitAll
         */
        @Suppress("NestedBlockDepth") // nesting is required here
        override suspend fun launch() {
            var cause: Throwable? = null
            var previousExecution: Job? = null
            var previousDeferred: CompletableDeferred<*>? = null
            try {
                while (true) {
                    // TODO: Check the spill state slots here (see emitAll for details)
                    // Manual kludge with `run` to fix retention of the last emitted value. See emitAll for details.
                    val result = run { requestsChannel.receiveCatching() }
                    if (result.isClosed) {
                        // previousFlow/previousDeferred are closed in the catch clause
                        result.exceptionOrNull()?.let { throw it }
                        break
                    }

                    if (previousExecution != null) {
                        /** @see kotlinx.coroutines.flow.internal.ChildCancelledException */
                        val ce = CancellationException("Previous intent was cancelled")
                        previousExecution.cancel(ce)
                        previousDeferred?.cancel(ce)

                        if (ordered) {
                            previousExecution.join()
                        }
                    }

                    val r = result.getOrThrow()
                    previousDeferred = r.deferred

                    // CoroutineStart.UNDISPATCHED could be better as we don't need dispatching for processing,
                    // but unlike transformLatest, we use dispatching so that the intent cannot block its own cancellation.
                    previousExecution = handler.launch(context = r.deferred ?: EmptyCoroutineContext, start = CoroutineStart.DEFAULT) {
                        executeIntent(r.intent, r.deferred)
                    }
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                cause = e
                previousExecution?.cancel(e.toCancellationException())
                previousDeferred?.completeExceptionally(e)
                throw e
            } finally {
                requestsChannel.cancelConsumed(cause)
            }
        }
    }
}
