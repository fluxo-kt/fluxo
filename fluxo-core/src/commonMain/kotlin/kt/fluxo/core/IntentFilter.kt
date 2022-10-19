package kt.fluxo.core

/**
 *
 * @return `true` to accept intent, `false` otherwise
 */
public typealias IntentFilter<Intent, State> = (state: State, intent: Intent) -> Boolean

