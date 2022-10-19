package kt.fluxo.core

/**
 * Basic `Redux`-like MVI reducer that recieves intent and current state providing the new state.
 */
// FIXME: DSL for usage (via IntentHandlerFromReducer)
public typealias Reducer<State, Intent> = State.(Intent) -> State

