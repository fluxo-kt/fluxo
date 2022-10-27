package kt.fluxo.core

/**
 * Basic `Redux`-like MVI reducer that receives intent and current state providing the new state.
 *
 * @see IntentHandler
 */
public typealias Reducer<Intent, State> = State.(Intent) -> State

