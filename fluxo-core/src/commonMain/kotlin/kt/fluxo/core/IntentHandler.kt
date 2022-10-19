package kt.fluxo.core

import kt.fluxo.core.dsl.StoreScope

/**
 * MVVM+ mix of `Executor` and [Reducer] from MVI for handling intents sent to the [Store].
 * You can use anything from [StoreScope] while handling.
 */
public fun interface IntentHandler<Intent, State, out SideEffect : Any> {

    public fun StoreScope<Intent, State, SideEffect>.handleIntent(intent: Intent)
}
