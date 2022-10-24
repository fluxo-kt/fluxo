package kt.fluxo.core

import kotlinx.coroutines.CoroutineScope
import kt.fluxo.core.annotation.FluxoDsl
import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.internal.FluxoIntent
import kt.fluxo.core.internal.FluxoIntentHandler
import kt.fluxo.core.internal.FluxoStore
import kt.fluxo.core.internal.ReducerIntentHandler
import kt.fluxo.core.internal.build
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// CoroutineScope extensions for Store

/**
 * Returns MVVM+ [Store] connected to [CoroutineScope] lifecycle.
 */
public inline fun <State, SideEffect : Any> CoroutineScope.store(
    initialState: State,
    settings: FluxoSettings<*, State, SideEffect>.() -> Unit = {},
): Store<StoreScope<Nothing, State, SideEffect>.() -> Unit, State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return kt.fluxo.core.store(initialState, settings = {
        eventLoopContext = coroutineContext
        settings()
    })
}

/**
 * Returns basic [Reducer]-based [Store] connected to [CoroutineScope] lifecycle.
 */
public inline fun <Intent, State, SideEffect : Any> CoroutineScope.store(
    initialState: State,
    noinline reducer: Reducer<Intent, State>,
    settings: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
): Store<Intent, State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return store(initialState, ReducerIntentHandler(reducer), settings)
}

/**
 * Returns MVI [Store] with MVVM+ [IntentHandler] DSL
 */
public inline fun <Intent, State, SideEffect : Any> CoroutineScope.store(
    initialState: State,
    handler: IntentHandler<Intent, State, SideEffect>,
    settings: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
): Store<Intent, State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return kt.fluxo.core.store(initialState, handler, settings = {
        eventLoopContext = coroutineContext
        settings()
    })
}


// Basic Store variants

/**
 * Returns MVVM+ [Store]
 */
public inline fun <State, SideEffect : Any> store(
    initialState: State,
    settings: FluxoSettings<*, State, SideEffect>.() -> Unit = {},
): Store<StoreScope<Nothing, State, SideEffect>.() -> Unit, State, SideEffect> {
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
 * Returns basic [Reducer]-based [Store]
 */
public inline fun <Intent, State, SideEffect : Any> store(
    initialState: State,
    noinline reducer: Reducer<Intent, State>,
    settings: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
): Store<Intent, State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return store(initialState, ReducerIntentHandler(reducer), settings)
}

/**
 * Returns MVI [Store] with MVVM+ [IntentHandler] DSL
 */
public inline fun <Intent, State, SideEffect : Any> store(
    initialState: State,
    handler: IntentHandler<Intent, State, SideEffect>,
    settings: FluxoSettings<Intent, State, SideEffect>.() -> Unit = {},
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


// Store DSL

/**
 * Build and execute an intent on [Store].
 */
@FluxoDsl
public fun <State, SideEffect : Any> StoreHost<State, SideEffect>.intent(
    intent: StoreScope<Nothing, State, SideEffect>.() -> Unit,
) {
    store.send(intent)
}
