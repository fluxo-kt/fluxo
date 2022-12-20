package kt.fluxo.core

/**
 *
 * @return `true` to accept intent, `false` otherwise
 */
@Deprecated("Use interceptor instead")
public typealias IntentFilter<Intent, State> = State.(intent: Intent) -> Boolean

