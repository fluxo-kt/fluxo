@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.factory

import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.IntentHandler
import kt.fluxo.core.SideEffectsStrategy
import kt.fluxo.core.Store
import kt.fluxo.core.StoreSE
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.annotation.ThreadSafe
import kt.fluxo.core.container
import kt.fluxo.core.debug.DEBUG
import kt.fluxo.core.debug.DebugStoreDecorator
import kt.fluxo.core.store
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
            require(settings.sideEffectsStrategy == SideEffectsStrategy.DISABLE) {
                "Expected SideEffectsStrategy.DISABLE to be set as a sideEffectsStrategy. " +
                    "Please, fill an issue if you think it should be done automatically here."
            }
        }
        return create<Intent, State, Nothing>(initialState, handler, settings)
    }
}
