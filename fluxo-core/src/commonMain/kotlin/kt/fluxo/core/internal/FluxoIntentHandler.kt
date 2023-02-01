@file:JvmName("FluxoIntentHandler")

package kt.fluxo.core.internal

import kt.fluxo.core.FluxoIntent
import kt.fluxo.core.IntentHandler
import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.dsl.StoreScope
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.native.ObjCName

/**
 * [IntentHandler] for functional MVVM+ [intents][FluxoIntent] that work as a handlers themselves.
 */
private object FluxoHandler : IntentHandler<FluxoIntent<Any?, Any>, Any?, Any> {

    override suspend fun StoreScope<FluxoIntent<Any?, Any>, Any?, Any>.handleIntent(intent: FluxoIntent<Any?, Any>) {
        intent()
    }
}

/**
 * Returns [IntentHandler] for functional MVVM+ [intents][FluxoIntent] that work as a handlers themselves.
 */
@PublishedApi
@InternalFluxoApi
@JvmName("create")
@JsName("FluxoIntentHandler")
@ObjCName("FluxoIntentHandler")
@Suppress("FunctionNaming", "FunctionName")
internal fun <S, SE : Any> FluxoIntentHandler(): IntentHandler<FluxoIntent<S, SE>, S, SE> {
    @Suppress("UNCHECKED_CAST")
    return FluxoHandler as IntentHandler<FluxoIntent<S, SE>, S, SE>
}
