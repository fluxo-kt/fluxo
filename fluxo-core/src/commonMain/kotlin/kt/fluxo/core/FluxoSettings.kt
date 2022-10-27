@file:Suppress("PropertyName", "VariableNaming", "MagicNumber", "MemberVisibilityCanBePrivate")

package kt.fluxo.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kt.fluxo.core.annotation.NotThreadSafe
import kt.fluxo.core.debug.DEBUG
import kt.fluxo.core.intercept.FluxoEvent
import kt.fluxo.core.internal.InputStrategyGuardian
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@NotThreadSafe
public class FluxoSettings<Intent, State, SideEffect : Any> {
    public var name: String? = null

    /**
     * If true the [Store] will be started only on first subscriber.
     */
    public var lazy: Boolean = true

    /**
     * Disable if you want [Store] to keep working after some exception happened in processing.
     *
     * @see exceptionHandler
     */
    public var closeOnExceptions: Boolean = true

    /**
     * Enables all the debug checks in the [Store].
     *
     * Disable explicitly if you need to bypass the [InputStrategyGuardian] checks, and sure in it.
     */
    public var debugChecks: Boolean = DEBUG

    // FIXME: TODO
    public var repeatOnSubscribedStopTimeout: Long = 100L

    /**
     * Either a positive [SideEffect]s channel capacity or one of the constants defined in [Channel.Factory].
     */
    public var sideEffectBufferSize: Int = Channel.BUFFERED


    // region Processing control

    public var bootstrapper: Bootstrapper<in Intent, State, SideEffect>? = null

    /**
     * Additional [interceptors][FluxoInterceptor] for the [Store] events.
     * Attach loggers, time-travelling, analytics, everything you want.
     */
    public val interceptors: MutableList<FluxoInterceptor<Intent, State, SideEffect>> = mutableListOf()

    public inline fun interceptor(crossinline onNotify: (event: FluxoEvent<Intent, State, SideEffect>) -> Unit) {
        interceptors.add(FluxoInterceptor(onNotify))
    }

    /**
     * If you need to filter out some [Intent]s.
     *
     * (`true` to accept intent, `false` otherwise)
     */
    public var intentFilter: IntentFilter<in Intent, State>? = null

    /**
     * Take full control of the intent processing pipeline.
     * [Fifo], [Lifo], and [Parallel] strategies available out of the box.
     * Or you can create your own if you need.
     */
    public var inputStrategy: InputStrategy<Intent, State> = Fifo

    // endregion


    // region Coroutines control

    /**
     * Additional 'parent' [CoroutineContext] for running store. It will be added to [eventLoopContext].
     */
    public var context: CoroutineContext = EmptyCoroutineContext

    /**
     *  [CoroutineDispatcher] or any [CoroutineContext] to run the [Store] event loop.
     */
    public var eventLoopContext: CoroutineContext = Dispatchers.Default
    public var intentContext: CoroutineContext = EmptyCoroutineContext
    public var sideJobsContext: CoroutineContext = EmptyCoroutineContext
    public var interceptorContext: CoroutineContext = EmptyCoroutineContext

    /**
     * [CoroutineExceptionHandler] to receive exception happened in processing.
     * Disable [closeOnExceptions] if you want [Store] to keep working in such situations.
     *
     * @see closeOnExceptions
     */
    public var exceptionHandler: CoroutineExceptionHandler? = null

    // endregion


    // region Accessors for in-box input strategies

    /**
     * Ordered processing strategy. Predictable and intuitive. Best for background.
     * Consider [Fifo] for the UI or more responsiveness instead.
     */
    public inline val Fifo: InputStrategy<Intent, State> get() = InputStrategy.Fifo()

    /**
     * UI events oriented strategy. Cancels previous unfinished intents when receives new one.
     *
     * Provides more responsiveness, but can lose some intents!
     */
    public inline val Lifo: InputStrategy<Intent, State> get() = InputStrategy.Lifo()

    /** Parallel processing of all intents. No guarantee that inputs will be processed in any given order. */
    public inline val Parallel: InputStrategy<Intent, State> get() = InputStrategy.Parallel()

    // endregion
}
