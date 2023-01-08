@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "NON_EXPORTABLE_TYPE", "PropertyName", "VariableNaming")

package kt.fluxo.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.annotation.FluxoDsl
import kt.fluxo.core.annotation.NotThreadSafe
import kt.fluxo.core.debug.DEBUG
import kt.fluxo.core.input.InputStrategy
import kt.fluxo.core.internal.InputStrategyGuardian
import kt.fluxo.core.internal.RunningSideJob.Companion.BOOTSTRAPPER_SIDE_JOB
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.internal.InlineOnly
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.native.ObjCName


public typealias FluxoSettingsS<Intent, State> = FluxoSettings<Intent, State, Nothing>


/**
 * Settings for the Fluxo store.
 *
 * * Change settings once and for all at one place with [FluxoSettings.DEFAULT].
 * * Provide a prepared settings object for the group of your stores
 *   (use [FluxoSettings()][FluxoSettings.invoke] and then the standard DSL).
 * * Configure each [store] individually with the standard DSL (see [container] and [store]).
 */
@FluxoDsl
@JsExport
@NotThreadSafe
public class FluxoSettings<Intent, State, SideEffect : Any> private constructor() {

    public var name: String? = null

    /**
     * If true the [Store] started only on first subscriber.
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
    @JsName("onStart")
    public inline fun onStart(noinline bootstrapper: Bootstrapper<in Intent, State, SideEffect>) {
        this.bootstrapper = bootstrapper
    }

    /** [bootstrapper] convenience method */
    @InlineOnly
    @JsName("onCreate")
    @Deprecated("Use onStart instead", ReplaceWith("onStart(bootstrapper)"))
    public inline fun onCreate(noinline bootstrapper: Bootstrapper<in Intent, State, SideEffect>) {
        onStart(bootstrapper)
    }

    /** [bootstrapper] convenience method when you need only a [sideJob][kt.fluxo.core.dsl.StoreScope.sideJob] */
    @JsName("bootstrapperJob")
    public fun bootstrapperJob(
        key: String = BOOTSTRAPPER_SIDE_JOB,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: SideJob<Intent, State, SideEffect>,
    ) {
        this.bootstrapper = { sideJob(key, context, start, block) }
    }


    /**
     * If you need to filter out some [Intent]s.
     * (`true` to accept intent, `false` otherwise)
     */
    @ExperimentalFluxoApi
    public var intentFilter: IntentFilter<in Intent, State>? = null

    /**
     * Take full control of the intent processing pipeline.
     * By default, intent is processed directly in the caller thread,
     * but any suspention will be dispatched in the [coroutineContext].
     *
     * Select from [Fifo], [Lifo], and [Parallel] strategies,
     * or create your own if you need.
     */
    public var inputStrategy: InputStrategy.Factory = Direct

    /**
     * A strategy for sharing side effects.
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
     * Extra 'parent' [CoroutineScope] for running store. Added to [coroutineContext].
     */
    public var scope: CoroutineScope? = null

    /**
     * [CoroutineDispatcher] or any [CoroutineContext] to run the main [Store] event loop.
     */
    public var coroutineContext: CoroutineContext = Dispatchers.Default

    /**
     * [CoroutineDispatcher] or any [CoroutineContext] to run the [Store] side jobs (long-running tasks).
     * By default, it uses the main [coroutineContext].
     */
    public var sideJobsContext: CoroutineContext = EmptyCoroutineContext

    /**
     * If false, the [Store] offload everything possible to the provided [scope],
     * not trying to optimize performance.
     */
    @get:JvmName("isOptimized")
    public var optimized: Boolean = true

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
            if (value != null) {
                closeOnExceptions = false
            }
        }

    /**
     * [exceptionHandler] convenience method
     *
     * **NOTE:** Disables [closeOnExceptions] when set!
     */
    @InlineOnly
    @JsName("setExceptionHandler")
    @JvmName("setExceptionHandler")
    public inline fun exceptionHandler(crossinline handler: (CoroutineContext, Throwable) -> Unit) {
        exceptionHandler = CoroutineExceptionHandler(handler)
    }

    /**
     * [exceptionHandler] convenience method
     *
     * **NOTE:** Disables [closeOnExceptions] when set!
     */
    @InlineOnly
    @JsName("onError")
    public inline fun onError(crossinline handler: CoroutineContext.(Throwable) -> Unit) {
        exceptionHandler = CoroutineExceptionHandler(handler)
    }

    // endregion


    // region Accessors for in-box input strategies

    /**
     * **Fifo**, `first-in, first-out` – ordered processing strategy.
     * Predictable and intuitive, default choice.
     * Based on [Channel] to keep the order of processing.
     *
     * **IMPORTANT:** The new intent starts execution only after all earlier intents finish!
     *
     * Consider [Parallel] or [Lifo] instead if you need more responsiveness.
     *
     * @see Parallel
     * @see Lifo
     * @see ChannelLifo
     */
    @InlineOnly
    @JsName("Fifo")
    @ObjCName("Fifo")
    @get:JvmName("Fifo")
    public inline val Fifo: InputStrategy.Factory get() = InputStrategy.Fifo

    /**
     * **Lifo**, `last-in, first-out` strategy.
     * Optimized for lots of events (for example, user actions) when new ones
     * make earlier ones obsolete.
     * Provides more responsiveness than [Fifo], but can lose some intents!
     *
     * **IMPORTANT:** Cancels earlier unfinished intent when receives a new one!
     *
     * **IMPORTANT:** No guarantee that inputs processed in any given order!
     *
     * Consider [Parallel] if you need more responsiveness, but without dropping out anything.
     *
     * See [ChannelLifo] for [Channel]-based implementation of the same strategy,
     * which waits for the last intent to complete after canceling it,
     * before processing a new one, making the strategy strictly ordered.
     *
     * @see Parallel
     * @see ChannelLifo
     * @see Fifo
     */
    @InlineOnly
    @JsName("Lifo")
    @ObjCName("Lifo")
    @get:JvmName("Lifo")
    public inline val Lifo: InputStrategy.Factory get() = InputStrategy.Lifo

    /**
     * Strategy with parallel processing of intents.
     * Can provide better responsiveness comparing to [Fifo].
     *
     * Use [CoroutineDispatcher.limitedParallelism] on [coroutineContext] or [scope] to limit parallelism to some value.
     *
     * **IMPORTANT:** No guarantee that inputs processed in any given order!
     *
     * @see Fifo
     * @see Lifo
     * @see ChannelLifo
     */
    @InlineOnly
    @JsName("Parallel")
    @ObjCName("Parallel")
    @get:JvmName("Parallel")
    public inline val Parallel: InputStrategy.Factory get() = InputStrategy.Parallel

    /**
     * [Parallel] strategy that will immediately execute intent until its first suspension point in the current thread
     * similarly to the coroutine being started using [Dispatchers.Unconfined].
     * However, when the coroutine is resumed from suspension it is dispatched
     * according to the [CoroutineDispatcher] in its context.
     *
     * @see CoroutineStart.UNDISPATCHED
     * @see Parallel
     */
    @InlineOnly
    @ExperimentalFluxoApi
    @JsName("Direct")
    @ObjCName("Direct")
    @get:JvmName("Direct")
    public inline val Direct: InputStrategy.Factory get() = InputStrategy.Direct

    /**
     * [Channel]-based implementation for the **[Lifo]**, `Last-in, first-out` strategy.
     * Optimized for a stream of events (e.g., user actions) in which new ones make previous ones obsolete.
     * Provides more responsiveness than [Fifo], but at the cost of possibly losing some intents!
     *
     * **IMPORTANT:** Cancels earlier unfinished intent when receives a new one!
     *
     * **IMPORTANT:** No guarantee that inputs processed in any given order!
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
    @InlineOnly
    @ExperimentalFluxoApi
    @Suppress("FunctionName")
    @JsName("ChannelLifo")
    @ObjCName("ChannelLifo")
    @JvmName("ChannelLifo")
    internal inline fun ChannelLifo(ordered: Boolean = true): InputStrategy.Factory = InputStrategy.ChannelLifo(ordered)

    // endregion


    // region Migration helpers

    /**
     * Not applicable from here! Use stopTimeout parameter for the [repeatOnSubscription] method instead.
     *
     * @see repeatOnSubscription
     */
    @InlineOnly
    @Deprecated("Not applicable from here! Use stopTimeout parameter for repeatOnSubscription method instead")
    public var repeatOnSubscribedStopTimeout: Long
        inline get() = 0
        inline set(@Suppress("UNUSED_PARAMETER") value) {}

    // endregion


    public fun copy(): FluxoSettings<Intent, State, SideEffect> {
        val s = FluxoSettings<Intent, State, SideEffect>()

        // copy values in reverse order to mitigate logic in setters

        s.exceptionHandler = exceptionHandler

        s.optimized = optimized
        s.sideJobsContext = sideJobsContext
        s.coroutineContext = coroutineContext
        s.scope = scope

        s.sideEffectsStrategy = sideEffectsStrategy
        s.inputStrategy = inputStrategy
        s.intentFilter = intentFilter
        s.bootstrapper = bootstrapper

        s.sideEffectBufferSize = sideEffectBufferSize
        s.debugChecks = debugChecks
        s.closeOnExceptions = closeOnExceptions
        s.lazy = lazy
        s.name = name

        return s
    }

    @NotThreadSafe
    public companion object Factory {
        /**
         * Global default [FluxoSettings]. Not thread safe!
         */
        @JvmField
        @JsName("DEFAULT")
        @ObjCName("DEFAULT")
        public val DEFAULT: FluxoSettings<out Any?, out Any?, out Any> = FluxoSettings()

        /**
         * Creates a new typed copy of [global default][DEFAULT] [FluxoSettings].
         */
        @JsName("create")
        @JvmName("create")
        @ObjCName("create")
        public operator fun <Intent, State, SideEffect : Any> invoke(): FluxoSettings<Intent, State, SideEffect> {
            @Suppress("UNCHECKED_CAST")
            return DEFAULT.copy() as FluxoSettings<Intent, State, SideEffect>
        }
    }
}
