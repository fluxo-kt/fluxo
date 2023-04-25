@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "TooManyFunctions", "TrailingCommaOnCallSite")

package kt.fluxo.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kt.fluxo.core.annotation.FluxoDsl
import kt.fluxo.core.dsl.ContainerHost
import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.factory.FluxoStoreFactory
import kt.fluxo.core.factory.StoreFactory
import kt.fluxo.core.internal.FluxoIntentHandler
import kt.fluxo.core.internal.ReducerIntentHandler
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.InlineOnly
import kotlin.internal.LowPriorityInOverloadResolution
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.native.ObjCName


// Convenience DSL for Fluxo usage (inline)


// region CoroutineScope extensions for Store

/**
 * Returns MVVM+ [Store], connected to [CoroutineScope] lifecycle.
 */
@InlineOnly
public inline fun <State> CoroutineScope.container(
    initialState: State,
    settings: FluxoSettings<FluxoIntentS<State>, State, Nothing>? = null,
    factory: StoreFactory? = null,
    @BuilderInference setup: FluxoSettings<FluxoIntentS<State>, State, Nothing>.() -> Unit = {},
): ContainerS<State> {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    val context = coroutineContext
    return kt.fluxo.core.container(
        initialState = initialState,
        settings = settings,
        factory = factory,
    ) {
        coroutineContext = context
        setup()
    }
}

/**
 * Returns MVVM+ [Store] with [SideEffect] support, connected to [CoroutineScope] lifecycle.
 */
@InlineOnly
@LowPriorityInOverloadResolution
@JsName("scopedContainerWithSideEffects")
@JvmName("containerWithSideEffects")
@ObjCName("containerWithSideEffects")
public inline fun <State, SideEffect : Any> CoroutineScope.container(
    initialState: State,
    settings: FluxoSettings<FluxoIntent<State, SideEffect>, State, SideEffect>? = null,
    factory: StoreFactory? = null,
    @BuilderInference setup: FluxoSettings<FluxoIntent<State, SideEffect>, State, SideEffect>.() -> Unit = {},
): Container<State, SideEffect> {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    val context = coroutineContext
    return kt.fluxo.core.container(
        initialState = initialState,
        settings = settings,
        factory = factory,
    ) {
        coroutineContext = context
        setup()
    }
}

/**
 * Returns basic [Reducer]-based [Store], connected to [CoroutineScope] lifecycle.
 *
 * Use [container] for MVVM+ [Store].
 */
@InlineOnly
public inline fun <Intent, State> CoroutineScope.store(
    initialState: State,
    @BuilderInference noinline reducer: Reducer<Intent, State>,
    settings: FluxoSettingsS<Intent, State>? = null,
    factory: StoreFactory? = null,
    @BuilderInference setup: FluxoSettingsS<Intent, State>.() -> Unit = {},
): Store<Intent, State> {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return store(
        initialState = initialState,
        handler = ReducerIntentHandler(reducer),
        settings = settings,
        factory = factory,
        setup = setup,
    )
}

/**
 * Returns MVI [Store] with MVVM+ [IntentHandler] DSL, connected to [CoroutineScope] lifecycle.
 *
 * Use [container] for MVVM+ [Store].
 */
@InlineOnly
public inline fun <Intent, State> CoroutineScope.store(
    initialState: State,
    @BuilderInference handler: IntentHandler<Intent, State, Nothing>,
    settings: FluxoSettingsS<Intent, State>? = null,
    factory: StoreFactory? = null,
    @BuilderInference setup: FluxoSettingsS<Intent, State>.() -> Unit = {},
): Store<Intent, State> {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    val context = coroutineContext
    return kt.fluxo.core.store(
        initialState = initialState,
        handler = handler,
        settings = settings,
        factory = factory,
    ) {
        coroutineContext = context
        setup()
    }
}

/**
 * Returns MVI [Store] with MVVM+ [IntentHandler] DSL and [SideEffect] support, connected to [CoroutineScope] lifecycle.
 *
 * Use [container] for MVVM+ [Store].
 */
@InlineOnly
@JsName("scopedStoreWithSideEffects")
@JvmName("storeWithSideEffects")
@ObjCName("storeWithSideEffects")
public inline fun <Intent, State, SideEffect : Any> CoroutineScope.store(
    initialState: State,
    @BuilderInference handler: IntentHandler<Intent, State, SideEffect>,
    settings: FluxoSettings<Intent, State, SideEffect>? = null,
    factory: StoreFactory? = null,
    @BuilderInference setup: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
): StoreSE<Intent, State, SideEffect> {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    val context = coroutineContext
    return kt.fluxo.core.store(
        initialState = initialState,
        handler = handler,
        settings = settings,
        factory = factory,
    ) {
        coroutineContext = context
        setup()
    }
}

// endregion


// region Basic Store variants

/**
 * Returns MVVM+ [Store].
 */
@InlineOnly
public inline fun <State> container(
    initialState: State,
    settings: FluxoSettings<FluxoIntentS<State>, State, Nothing>? = null,
    factory: StoreFactory? = null,
    @BuilderInference setup: FluxoSettings<FluxoIntentS<State>, State, Nothing>.() -> Unit = {},
): ContainerS<State> {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return container<State, Nothing>(
        initialState = initialState,
        settings = settings,
        factory = factory,
    ) {
        sideEffectStrategy = SideEffectStrategy.DISABLE
        setup()
    }
}

/**
 * Returns MVVM+ [Store] with [SideEffect] support.
 */
@InlineOnly
@LowPriorityInOverloadResolution
@JsName("containerWithSideEffects")
@JvmName("containerWithSideEffects")
@ObjCName("containerWithSideEffects")
public inline fun <State, SideEffect : Any> container(
    initialState: State,
    settings: FluxoSettings<FluxoIntent<State, SideEffect>, State, SideEffect>? = null,
    factory: StoreFactory? = null,
    @BuilderInference setup: FluxoSettings<FluxoIntent<State, SideEffect>, State, SideEffect>.() -> Unit = {},
): Container<State, SideEffect> {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return store(
        initialState = initialState,
        handler = FluxoIntentHandler(),
        settings = settings,
        factory = factory,
        setup = setup,
    )
}

/**
 * Returns basic [Reducer]-based [Store].
 *
 * Use [container] for MVVM+ [Store].
 */
@InlineOnly
public inline fun <Intent, State> store(
    initialState: State,
    @BuilderInference noinline reducer: Reducer<Intent, State>,
    settings: FluxoSettingsS<Intent, State>? = null,
    factory: StoreFactory? = null,
    @BuilderInference setup: FluxoSettingsS<Intent, State>.() -> Unit = {},
): Store<Intent, State> {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return store(
        initialState = initialState,
        handler = ReducerIntentHandler(reducer),
        settings = settings,
        factory = factory,
        setup = setup,
    )
}

/**
 * Returns MVI [Store] with MVVM+ [IntentHandler] DSL.
 *
 * Use [container] for MVVM+ [Store].
 */
@InlineOnly
public inline fun <Intent, State> store(
    initialState: State,
    @BuilderInference handler: IntentHandler<Intent, State, Nothing>,
    settings: FluxoSettingsS<Intent, State>? = null,
    factory: StoreFactory? = null,
    @BuilderInference setup: FluxoSettingsS<Intent, State>.() -> Unit = {},
): Store<Intent, State> {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return (factory ?: FluxoStoreFactory).create(
        initialState = initialState,
        handler = handler,
        // Always do a copy to not change the original settings.
        settings = (settings?.copy() ?: FluxoSettings()).apply {
            sideEffectStrategy = SideEffectStrategy.DISABLE
            setup()
        },
    )
}

/**
 * Returns MVI [Store] with MVVM+ [IntentHandler] DSL and [SideEffect] support.
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
    factory: StoreFactory? = null,
    @BuilderInference setup: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
): StoreSE<Intent, State, SideEffect> {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return (factory ?: FluxoStoreFactory).create(
        initialState = initialState,
        handler = handler,
        // Always do a copy to not change the original settings.
        settings = (settings?.copy() ?: FluxoSettings()).apply(setup),
    )
}

// endregion


// region Store DSL

/**
 * Build and execute a functional [intent][FluxoIntent] on [Store].
 *
 * Use [send] if you need an intent [Job].
 */
@FluxoDsl
@InlineOnly
public inline fun <State, SE : Any> ContainerHost<State, SE>.intent(noinline intent: FluxoIntent<State, SE>) {
    container.send(intent)
}

/**
 * Build and execute a functional [intent][FluxoIntent] on [Store], returning a [Job].
 */
@FluxoDsl
@InlineOnly
public inline fun <State, SE : Any> ContainerHost<State, SE>.send(noinline intent: FluxoIntent<State, SE>): Job = container.send(intent)

/**
 * Build and execute a functional [intent][FluxoIntent] on [Store].
 *
 * Use [send] if you need an intent [Job].
 */
@FluxoDsl
@InlineOnly
public inline fun <State, SE : Any> Container<State, SE>.intent(noinline intent: FluxoIntent<State, SE>) {
    send(intent)
}


/**
 * Updates the [StoreScope.value] atomically using the specified [reducer] of its value.
 *
 * [reducer] may be evaluated multiple times, if [Store.value] is being concurrently updated.
 *
 * @see MutableStateFlow.update
 * @see MutableStateFlow.compareAndSet
 * @see StoreScope.compareAndSet
 *
 * @TODO Lint/Detekt rules with recommendation to avoid this method when the prevState is not used
 */
@FluxoDsl
@InlineOnly
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline fun <State> StoreScope<*, State, *>.updateState(reducer: (prevState: State) -> State): State {
    while (true) {
        val prevValue = value
        val nextValue = reducer(prevValue)
        if (compareAndSet(prevValue, nextValue)) {
            return nextValue
        }
    }
}

// endregion
