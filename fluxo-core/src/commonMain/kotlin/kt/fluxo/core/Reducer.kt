package kt.fluxo.core

/**
 * Basic `Redux`-like MVI reducer that recieves intent and current state providing the new state.
 */
public typealias Reducer<Intent, State> = State.(Intent) -> State

