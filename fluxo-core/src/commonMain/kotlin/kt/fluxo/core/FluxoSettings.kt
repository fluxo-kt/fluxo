@file:Suppress("UNCHECKED_CAST", "PropertyName", "VariableNaming", "MagicNumber", "MemberVisibilityCanBePrivate")

package kt.fluxo.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kt.fluxo.core.annotation.NotThreadSafe
import kt.fluxo.core.debug.DEBUG
import kt.fluxo.core.strategy.FifoInputStrategy
import kt.fluxo.core.strategy.LifoInputStrategy
import kt.fluxo.core.strategy.ParallelInputStrategy
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
     * Enables all the debug checks in application
     */
    public var debugChecks: Boolean = DEBUG

    // FIXME: TODO
    public var repeatOnSubscribedStopTimeout: Long = 100L

    /**
     * Either a positive [SideEffect]s channel capacity or one of the constants defined in [Channel.Factory].
     */
    public var sideEffectBufferSize: Int = Channel.BUFFERED


    // region Processing control

    // FIXME: TODO
    public val bootstrapper: Bootstrapper<Intent, State, SideEffect>? = null

    /**
     * Additional [interceptors][FluxoInterceptor] for the [Store] events.
     * Attach loggers, time-travelling, analytics, everything you want.
     */
    public val interceptors: MutableList<FluxoInterceptor<Intent, SideEffect, State>> = mutableListOf()

    /**
     * In case you need to filter out some [Intent]s.
     */
    public var intentFilter: IntentFilter<Intent, State>? = null

    /**
     * Take full control of the intent processing pipeline.
     * [FIFO], [LIFO], and [PARALLEL] strategies available. You can use your own if needed.
     */
    public var inputStrategy: InputStrategy<Intent, State> = LIFO

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


    // region Accessers for in-box input strategies

    // background processing oriented strategy
    public val FIFO: InputStrategy<Intent, State> get() = FifoInputStrategy as InputStrategy<Intent, State>

    // UI events oriented
    public val LIFO: InputStrategy<Intent, State> get() = LifoInputStrategy as InputStrategy<Intent, State>

    // no guarantee that inputs will be processed in any given order
    public val PARALLEL: InputStrategy<Intent, State> get() = ParallelInputStrategy as InputStrategy<Intent, State>

    // endregion
}
