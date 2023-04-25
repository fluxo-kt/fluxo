@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.factory

import kt.fluxo.core.Container
import kt.fluxo.core.ContainerS
import kt.fluxo.core.FluxoIntent
import kt.fluxo.core.FluxoIntentS
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.IntentHandler
import kt.fluxo.core.Reducer
import kt.fluxo.core.SideEffectStrategy
import kt.fluxo.core.Store
import kt.fluxo.core.StoreSE
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.annotation.ThreadSafe
import kt.fluxo.core.container
import kt.fluxo.core.debug.DEBUG
import kt.fluxo.core.debug.DebugStoreDecorator
import kt.fluxo.core.internal.ReducerIntentHandler
import kt.fluxo.core.store
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.InlineOnly
import kotlin.internal.LowPriorityInOverloadResolution
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.native.ObjCName

/**
 * Creates instances of [Store]s using the provided components.
 * You can create different [Store] wrappers and combine them depending on circumstances.
 *
 * Expected to be thread safe.
 *
 * @see FluxoStoreFactory for default implementation
 */
@ThreadSafe
@ExperimentalFluxoApi
public abstract class StoreFactory {

    /**
     * Main method for implementation. Creates intsance of [StoreDecorator] useful for wrapping and other preparations.
     */
    @JsName("createForDecoration")
    public abstract fun <Intent, State, SideEffect : Any> createForDecoration(
        initialState: State,
        @BuilderInference handler: IntentHandler<Intent, State, SideEffect>,
        settings: FluxoSettings<Intent, State, SideEffect>,
    ): StoreDecorator<Intent, State, SideEffect>


    /**
     * Creates an implementation of [Store] with side effects.
     */
    @JsName("create")
    public fun <Intent, State, SideEffect : Any> create(
        initialState: State,
        @BuilderInference handler: IntentHandler<Intent, State, SideEffect>,
        settings: FluxoSettings<Intent, State, SideEffect> = FluxoSettings(),
    ): StoreSE<Intent, State, SideEffect> {
        val store = createForDecoration(initialState, handler, settings)
        store.init(store, settings)

        // Wrap in a special decorator to provide debug features
        if (DEBUG && settings.debugChecks) {
            /**
             * [DebugStoreDecorator] doesn't override anything internally,
             * so avoid it for the [StoreDecorator.init] call.
             */
            return DebugStoreDecorator(store)
        }

        return store
    }

    /**
     * Creates an implementation of [Store] with no side effects.
     * Consider to use [container] or [store] DSL methods instead!
     */
    @JsName("createWithNoSideEffects")
    @JvmName("createWithNoSideEffects")
    @ObjCName("createWithNoSideEffects")
    public fun <Intent, State> create(
        initialState: State,
        @BuilderInference handler: IntentHandler<Intent, State, Nothing>,
        settings: FluxoSettings<Intent, State, Nothing>,
    ): Store<Intent, State> {
        if (settings.debugChecks) {
            require(settings.sideEffectStrategy == SideEffectStrategy.DISABLE) {
                "Expected SideEffectsStrategy.DISABLE to be set as a sideEffectsStrategy. " +
                    "Please, fill an issue if you think it should be done automatically here."
            }
        }
        return create<Intent, State, Nothing>(initialState, handler, settings)
    }


    // region StoreFactory extensions for Store

    /**
     * Returns MVVM+ [Store], created with current [StoreFactory].
     */
    @InlineOnly
    @JsName("container")
    public inline fun <State> container(
        initialState: State,
        settings: FluxoSettings<FluxoIntentS<State>, State, Nothing>? = null,
        @BuilderInference setup: FluxoSettings<FluxoIntentS<State>, State, Nothing>.() -> Unit = {},
    ): ContainerS<State> {
        contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
        return container(
            initialState = initialState,
            settings = settings,
            factory = this,
            setup = setup,
        )
    }

    /**
     * Returns MVVM+ [Store] with [SideEffect] support, created with current [StoreFactory].
     */
    @InlineOnly
    @LowPriorityInOverloadResolution
    @JsName("containerWithSideEffects")
    @JvmName("containerWithSideEffects")
    @ObjCName("containerWithSideEffects")
    public inline fun <State, SideEffect : Any> container(
        initialState: State,
        settings: FluxoSettings<FluxoIntent<State, SideEffect>, State, SideEffect>? = null,
        @BuilderInference setup: FluxoSettings<FluxoIntent<State, SideEffect>, State, SideEffect>.() -> Unit = {},
    ): Container<State, SideEffect> {
        contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
        return container(
            initialState = initialState,
            settings = settings,
            factory = this,
            setup = setup,
        )
    }

    /**
     * Returns basic [Reducer]-based [Store], created with current [StoreFactory].
     *
     * Use [container] for MVVM+ [Store].
     */
    @InlineOnly
    @JsName("storeWithReducer")
    public inline fun <Intent, State> store(
        initialState: State,
        @BuilderInference noinline reducer: Reducer<Intent, State>,
        settings: FluxoSettings<Intent, State, Nothing>? = null,
        @BuilderInference setup: FluxoSettings<Intent, State, Nothing>.() -> Unit = {},
    ): Store<Intent, State> {
        contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
        return store(
            initialState = initialState,
            handler = ReducerIntentHandler(reducer),
            settings = settings,
            factory = this,
            setup = setup,
        )
    }

    /**
     * Returns MVI [Store] with MVVM+ [IntentHandler] DSL, created with current [StoreFactory].
     *
     * Use [container] for MVVM+ [Store].
     */
    @InlineOnly
    @JsName("store")
    public inline fun <Intent, State> store(
        initialState: State,
        @BuilderInference handler: IntentHandler<Intent, State, Nothing>,
        settings: FluxoSettings<Intent, State, Nothing>? = null,
        @BuilderInference setup: FluxoSettings<Intent, State, Nothing>.() -> Unit = {},
    ): Store<Intent, State> {
        contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
        return store(
            initialState = initialState,
            handler = handler,
            settings = settings,
            factory = this,
            setup = setup,
        )
    }

    /**
     * Returns MVI [Store] with MVVM+ [IntentHandler] DSL and [SideEffect] support, created with current [StoreFactory].
     *
     * Use [container] for MVVM+ [Store].
     */
    @InlineOnly
    @JsName("storeWithSideEffects")
    @JvmName("storeWithSideEffects")
    @ObjCName("storeWithSideEffects")
    public inline fun <Intent, State, SideEffect : Any> store(
        initialState: State,
        @BuilderInference handler: IntentHandler<Intent, State, SideEffect>,
        settings: FluxoSettings<Intent, State, SideEffect>? = null,
        @BuilderInference setup: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
    ): StoreSE<Intent, State, SideEffect> {
        contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
        return store(
            initialState = initialState,
            handler = handler,
            settings = settings,
            factory = this,
            setup = setup,
        )
    }

    // endregion
}
