@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core

import kotlinx.coroutines.CoroutineScope
import kt.fluxo.core.annotation.FluxoDsl
import kt.fluxo.core.internal.FluxoIntentHandler
import kt.fluxo.core.internal.FluxoStore
import kt.fluxo.core.internal.ReducerIntentHandler
import kt.fluxo.core.internal.build
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.internal.InlineOnly


// Convenience DSL for Fluxo usage


// region CoroutineScope extensions for Store

/**
 * Returns MVVM+ [Store] connected to [CoroutineScope] lifecycle.
 */
public inline fun <State, SideEffect : Any> CoroutineScope.container(
    initialState: State,
    @BuilderInference settings: FluxoSettings<FluxoIntent<State, SideEffect>, State, SideEffect>.() -> Unit = {},
): Container<State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return kt.fluxo.core.container(initialState, settings = {
        eventLoopContext = coroutineContext
        settings()
    })
}

/**
 * Returns basic [Reducer]-based [Store] connected to [CoroutineScope] lifecycle.
 *
 * Use [container] for MVVM+ [Store].
 */
public inline fun <Intent, State, SideEffect : Any> CoroutineScope.store(
    initialState: State,
    @BuilderInference noinline reducer: Reducer<Intent, State>,
    @BuilderInference settings: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
): Store<Intent, State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return store(initialState, ReducerIntentHandler(reducer), settings)
}

/**
 * Returns MVI [Store] with MVVM+ [IntentHandler] DSL.
 *
 * Use [container] for MVVM+ [Store].
 */
public inline fun <Intent, State, SideEffect : Any> CoroutineScope.store(
    initialState: State,
    @BuilderInference handler: IntentHandler<Intent, State, SideEffect>,
    @BuilderInference settings: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
): Store<Intent, State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return kt.fluxo.core.store(initialState, handler, settings = {
        eventLoopContext = coroutineContext
        settings()
    })
}

// endregion


// region Basic Store variants

/**
 * Returns MVVM+ [Store].
 */
public inline fun <State, SideEffect : Any> container(
    initialState: State,
    @BuilderInference settings: FluxoSettings<FluxoIntent<State, SideEffect>, State, SideEffect>.() -> Unit = {},
): Container<State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return store(
        initialState,
        @Suppress("UNCHECKED_CAST") (FluxoIntentHandler as IntentHandler<FluxoIntent<State, SideEffect>, State, SideEffect>),
        settings,
    )
}

/**
 * Returns basic [Reducer]-based [Store].
 *
 * Use [container] for MVVM+ [Store].
 */
public inline fun <Intent, State, SideEffect : Any> store(
    initialState: State,
    @BuilderInference noinline reducer: Reducer<Intent, State>,
    @BuilderInference settings: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
): Store<Intent, State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return store(initialState, ReducerIntentHandler(reducer), settings)
}

/**
 * Returns MVI [Store] with MVVM+ [IntentHandler] DSL.
 *
 * Use [container] for MVVM+ [Store].
 */
public inline fun <Intent, State, SideEffect : Any> store(
    initialState: State,
    @BuilderInference handler: IntentHandler<Intent, State, SideEffect>,
    @BuilderInference settings: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
): Store<Intent, State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return FluxoStore(
        initialState = initialState,
        intentHandler = handler,
        conf = FluxoSettings<Intent, State, SideEffect>().apply(settings).build(),
    )
}

// endregion


// region Store DSL

/**
 * Build and execute a functional [intent][FluxoIntent] on [Store].
 */
@FluxoDsl
@InlineOnly
public inline fun <State, SideEffect : Any> ContainerHost<State, SideEffect>.intent(noinline intent: FluxoIntent<State, SideEffect>) {
    container.send(intent)
}

// endregion
