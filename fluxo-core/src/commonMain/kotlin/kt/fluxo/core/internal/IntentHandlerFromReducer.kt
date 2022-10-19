package kt.fluxo.core.internal

import kt.fluxo.core.IntentHandler
import kt.fluxo.core.Reducer
import kt.fluxo.core.dsl.StoreScope

/**
 * Wraps a [Reducer], making an [IntentHandler] from it.
 */
internal class IntentHandlerFromReducer<Intent, State, out SideEffect : Any>(
    private val reducer: Reducer<State, Intent>,
) : IntentHandler<Intent, State, SideEffect> {

    override fun StoreScope<Intent, State, SideEffect>.handleIntent(intent: Intent) {
        updateState { state ->
            reducer(state, intent)
        }
    }
}
