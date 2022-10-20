package kt.fluxo.core

import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.internal.FluxoIntent
import kt.fluxo.core.internal.FluxoIntentHandler
import kt.fluxo.core.internal.FluxoStore
import kt.fluxo.core.internal.ReducerIntentHandler
import kt.fluxo.core.internal.build
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Return MVVM+ store
 */
public inline fun <State, SideEffect : Any> store(
    initialState: State,
    settings: FluxoSettings<*, State, SideEffect>.() -> Unit = {},
): Store<StoreScope<*, State, SideEffect>.() -> Unit, State, SideEffect> {
    contract {
        callsInPlace(settings, InvocationKind.EXACTLY_ONCE)
    }
    return store(
        initialState,
        @Suppress("UNCHECKED_CAST")
        (FluxoIntentHandler as IntentHandler<FluxoIntent<State, SideEffect>, State, SideEffect>),
        settings,
    )
}

/**
 * Returns basic [Reducer]-based store
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
 * Returns MVI store with MVVP+ [IntentHandler] DSL
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
        conf = FluxoSettings<Intent, State, SideEffect>()
            .apply(settings)
            .build(),
    )
}
