@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "PropertyName", "VariableNaming")

package kt.fluxo.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kt.fluxo.core.annotation.FluxoDsl
import kt.fluxo.core.annotation.NotThreadSafe
import kt.fluxo.core.debug.DEBUG
import kt.fluxo.core.dsl.SideJobScope
import kt.fluxo.core.dsl.StoreScope.Companion.BOOTSTRAPPER_SIDE_JOB
import kt.fluxo.core.intercept.FluxoEvent
import kt.fluxo.core.internal.InputStrategyGuardian
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.internal.InlineOnly
import kotlin.jvm.JvmName

@FluxoDsl
@NotThreadSafe
public class FluxoSettings<Intent, State, SideEffect : Any> {
    public var name: String? = null

    /**
     * If true the [Store] will be started only on first subscriber.
     */
    @get:JvmName("isLazy")
    public var lazy: Boolean = true

    /**
     * Disable if you want [Store] to keep working after some exception happened in processing.
     *
     * @see exceptionHandler
     */
    @get:JvmName("isCloseOnExceptions")
    public var closeOnExceptions: Boolean = true

    /**
     * Enables all the debug checks in the [Store].
     *
     * Disable explicitly if you need to bypass the [InputStrategyGuardian] checks, and sure in it.
     *
     * [closeOnExceptions] is also enabled if [debugChecks] enabled explicitly.
     */
    public var debugChecks: Boolean = DEBUG
        set(value) {
            field = value
            if (value) closeOnExceptions = true
        }

    /**
     * Either a positive [SideEffect]s channel capacity or one of the constants defined in [Channel.Factory].
     * Used as a `extraBufferCapacity` parameter for [MutableSharedFlow] when [SideEffectsStrategy.SHARE] used.
     *
     * @see sideEffectsStrategy
     */
    public var sideEffectBufferSize: Int = Channel.BUFFERED


    // region Processing control

    public var bootstrapper: Bootstrapper<in Intent, State, SideEffect>? = null

    /** [bootstrapper] convenience method */
    @InlineOnly
    public inline fun onStart(noinline bootstrapper: Bootstrapper<in Intent, State, SideEffect>) {
        this.bootstrapper = bootstrapper
    }

    /** [bootstrapper] convenience method */
    @InlineOnly
    @Deprecated("Use onStart instead", ReplaceWith("onStart(bootstrapper)"))
    public inline fun onCreate(noinline bootstrapper: Bootstrapper<in Intent, State, SideEffect>) {
        onStart(bootstrapper)
    }

    /** [bootstrapper] convenience method when you need only a [sideJob][kt.fluxo.core.dsl.StoreScope.sideJob] */
    public fun bootstrapperJob(key: String = BOOTSTRAPPER_SIDE_JOB, block: suspend SideJobScope<Intent, State, SideEffect>.() -> Unit) {
        this.bootstrapper = { sideJob(key, block) }
    }


    /**
     * [Interceptors][FluxoInterceptor] for the [Store] events.
     * Attach loggers, time-travelling, analytics, everything you want.
     *
     * @see FluxoInterceptor
     */
    public val interceptors: MutableList<FluxoInterceptor<Intent, State, SideEffect>> = mutableListOf()

    /** [interceptors] convenience method */
    @InlineOnly
    public inline fun interceptor(crossinline onEvent: (event: FluxoEvent<Intent, State, SideEffect>) -> Unit) {
        interceptors.add(FluxoInterceptor(onEvent))
    }

    /** [interceptors] convenience method */
    @InlineOnly
    public inline fun onEvent(crossinline onEvent: (event: FluxoEvent<Intent, State, SideEffect>) -> Unit) {
        interceptor(onEvent)
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
    public var inputStrategy: InputStrategy = Fifo

    /**
     * A strategy to be applied when sharing side effects.
     * [sideEffectBufferSize] defaults to `0` when [SideEffectsStrategy.SHARE] used.
     *
     * @see SideEffectsStrategy
     * @see sideEffectBufferSize
     */
    public var sideEffectsStrategy: SideEffectsStrategy = SideEffectsStrategy.RECEIVE
        set(value) {
            field = value
            if (value is SideEffectsStrategy.SHARE && sideEffectBufferSize == Channel.BUFFERED) {
                sideEffectBufferSize = 0
            }
        }

    // endregion


    // region Coroutines control

    /**
     * Additional 'parent' [CoroutineScope] for running store. It will be added to [eventLoopContext].
     */
    public var scope: CoroutineScope? = null

    /**
     *  [CoroutineDispatcher] or any [CoroutineContext] to run the [Store] event loop.
     */
    public var eventLoopContext: CoroutineContext = Dispatchers.Default
    public var intentContext: CoroutineContext = EmptyCoroutineContext
    public var sideJobsContext: CoroutineContext = EmptyCoroutineContext
    public var interceptorContext: CoroutineContext = EmptyCoroutineContext

    /**
     * [CoroutineExceptionHandler] to receive exception happened in processing.
     *
     * **NOTE:** Disables [closeOnExceptions] when set to non-null value!
     *
     * @see closeOnExceptions
     */
    public var exceptionHandler: CoroutineExceptionHandler? = null
        set(value) {
            field = value
            closeOnExceptions = value == null
        }

    /** [exceptionHandler] convenience method */
    @InlineOnly
    public inline fun exceptionHandler(crossinline handler: (CoroutineContext, Throwable) -> Unit) {
        exceptionHandler = CoroutineExceptionHandler(handler)
    }

    // endregion


    // region Accessors for in-box input strategies

    /**
     * Ordered processing strategy. Predictable and intuitive. Best for background.
     * Consider [Lifo] for the UI or more responsiveness instead.
     */
    @get:JvmName("Fifo")
    public inline val Fifo: InputStrategy get() = InputStrategy.Fifo

    /**
     * UI events oriented strategy. Cancels previous unfinished intents when receives new one.
     * Provides more responsiveness, but can lose some intents!
     * Use [Fifo] if you need more predictable behavior.
     */
    @get:JvmName("Lifo")
    public inline val Lifo: InputStrategy get() = InputStrategy.Lifo

    /** Parallel processing of all intents. No guarantee that inputs will be processed in any given order. */
    @get:JvmName("Parallel")
    public inline val Parallel: InputStrategy get() = InputStrategy.Parallel

    // endregion


    // region Migration helpers

    /**
     * Not applicable from here. Use stopTimeout parameter for the [repeatOnSubscription] method instead.
     *
     * @see repeatOnSubscription
     */
    @Deprecated("Use stopTimeout parameter for repeatOnSubscription method instead")
    public var repeatOnSubscribedStopTimeout: Long = 0L

    // endregion
}
