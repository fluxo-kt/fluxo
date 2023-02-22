@file:Suppress("MatchingDeclarationName")
@file:JvmName("ReducerIntentHandler")

package kt.fluxo.core.internal

import kt.fluxo.core.IntentHandler
import kt.fluxo.core.Reducer
import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.dsl.StoreScope
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.native.ObjCName

/**
 * Wraps a [Reducer], making an [IntentHandler] from it.
 */
internal class ReducerHandler<Intent, State, SideEffect : Any>(
    private val reducer: Reducer<Intent, State>,
) : IntentHandler<Intent, State, SideEffect> {

    override suspend fun StoreScope<Intent, State, SideEffect>.handleIntent(intent: Intent) {
        updateState { state ->
            reducer(state, intent)
        }
    }
}

/**
 * Wraps a [Reducer], making an [IntentHandler] from it.
 */
@PublishedApi
@InternalFluxoApi
@JvmName("create")
@JsName("ReducerIntentHandler")
@ObjCName("ReducerIntentHandler")
@Suppress("FunctionNaming", "FunctionName")
internal fun <I, S, SE : Any> ReducerIntentHandler(reducer: Reducer<I, S>): IntentHandler<I, S, SE> {
    return ReducerHandler(reducer)
}
