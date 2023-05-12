package kt.fluxo.core.intent

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kt.fluxo.common.annotation.ExperimentalFluxoApi
import kt.fluxo.core.annotation.CallSuper
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.js.JsName
import kotlin.jvm.JvmField

/**
 * [IntentStrategy] that uses coroutines [Channel] to streamline [Intent]s.
 * Supports leak-free transfer via [Channel]. See "Undelivered elements" section in [Channel] documentation for details.
 *
 * **WARN:** Uses [createQueue] right in the constructor, be sure to override it using values available at construction time!
 *
 * **NOTE: please fill an issue if you need this class to be open for your own specific implementations!**
 *
 * @TODO: Causes JS_FAKE_NAME_CLASH warning on inheritors. Find how to avoid it.
 */
@ExperimentalFluxoApi
internal open class ChannelBasedIntentStrategy<Intent, State>(
    handler: IntentStrategyScope<Intent, State>,
    resendUndelivered: Boolean,
    parallelProcessing: Boolean = false,
) : IntentStrategy<Intent, State>(
    handler = handler,
    isLaunchNeeded = true,
    parallelProcessing = parallelProcessing,
), FlowCollector<IntentRequest<Intent>> {

    @JvmField
    protected val requestsChannel: Channel<IntentRequest<Intent>>

    init {
        // Leak-free transfer via channel
        // https://github.com/Kotlin/kotlinx.coroutines/issues/1936
        // See "Undelivered elements" section in Channel documentation for details.
        //
        // Handled cases:
        // — sending operation cancelled before it had a chance to actually send the element.
        // — receiving operation retrieved the element from the channel cancelled
        //   when trying to return it the caller.
        // — channel cancelled, in which case onUndeliveredElement called
        //   on every remaining element in the channel's buffer.
        val intentResendLock = if (resendUndelivered) Mutex() else null
        @Suppress("LeakingThis")
        requestsChannel = createQueue {
            var wasResent = false
            // Don't fall into the recursion, so only one resending per moment.
            if (intentResendLock?.tryLock() == true) {
                try {
                    @Suppress("UNINITIALIZED_VARIABLE")
                    val channel = requestsChannel
                    if (!channel.isClosedForSend) {
                        wasResent = channel.trySend(it).isSuccess
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                    this.handler.handleException(EmptyCoroutineContext, e)
                } finally {
                    intentResendLock.unlock()
                }
            }
            if (!wasResent) {
                it.deferred?.cancel()
            }
            this.handler.undeliveredIntent(it.intent, wasResent)
        }
    }

    /**
     * Creates a [Channel] with the specified buffer capacity (or without a buffer by default).
     * See [Channel] interface documentation for details.
     *
     * @param onUndeliveredElement See "Undelivered elements" section in [Channel] documentation for details.
     *
     * @see kotlinx.coroutines.channels.Channel
     */
    @JsName("createQueue")
    protected open fun <Request> createQueue(onUndeliveredElement: ((Request) -> Unit)?): Channel<Request> {
        return Channel(
            capacity = Channel.UNLIMITED,
            onBufferOverflow = BufferOverflow.SUSPEND,
            onUndeliveredElement = onUndeliveredElement,
        )
    }


    /**
     * Reads all elements from the [requestsChannel], calls [executeIntent] for each [Intent] via [emit] method.
     * [cancels][Channel.cancel] (consumes) the channel afterward.
     *
     * Note, that emitting values from a channel into a flow is not atomic. A value that was received from the
     * channel may not reach the flow collector if it was cancelled and will be lost.
     *
     * Uses [emitAll] as a more efficient shorthand for `channel.consumeEach { value -> emit(value) }`.
     * See [consumeEach][ReceiveChannel.consumeEach].
     *
     * @see ReceiveChannel.consumeEach
     * @see kotlinx.coroutines.flow.emitAll
     * @see kotlinx.coroutines.flow.emitAllImpl
     */
    override suspend fun launch() = emitAll(requestsChannel)

    /**
     * Helper method to use [IntentStrategy] itself as a [FlowCollector] for [emitAll],
     * without allocating extra objects.
     */
    final override suspend fun emit(value: IntentRequest<Intent>) {
        // Don't pay for dispatch here, it is never necessary.
        // It is also necessary not to add parallelism to the execution.
        handler.launch(context = value.deferred ?: EmptyCoroutineContext, start = CoroutineStart.UNDISPATCHED) {
            executeIntent(value.intent, value.deferred)
        }
    }

    final override suspend fun queueIntentSuspend(intent: Intent) = queueIntent(intent, deferred = null)

    final override fun queueIntent(intent: Intent): Job {
        val deferred = CompletableDeferred<Unit>(handler.coroutineContext[Job])
        // Don't pay for dispatch here, it is never necessary
        handler.launch(context = deferred, start = CoroutineStart.UNDISPATCHED) {
            // TODO: Test cancellation of suspended queuing if deferred was cancelled
            queueIntent(intent, deferred)
        }
        return deferred
    }

    private suspend fun queueIntent(intent: Intent, deferred: CompletableDeferred<Unit>?) {
        try {
            requestsChannel.send(IntentRequest(intent, deferred))
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            handler.undeliveredIntent(intent, wasResent = false)
            throw e
        }
    }


    @CallSuper
    final override fun close() {
        requestsChannel.close()
    }
}
