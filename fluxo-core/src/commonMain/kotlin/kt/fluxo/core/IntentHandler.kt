package kt.fluxo.core

import kt.fluxo.core.dsl.StoreScope

/**
 * MVVM+ mix of `Executor` and [Reducer] from MVI for handling intents sent to the [Store].
 * You can use anything from [StoreScope] while handling.
 *
 * @see Reducer
 */
public fun interface IntentHandler<Intent, State, out SideEffect : Any> {

    @Suppress("FUN_INTERFACE_WITH_SUSPEND_FUNCTION")
    public suspend fun StoreScope<Intent, State, SideEffect>.handleIntent(intent: Intent)
}
