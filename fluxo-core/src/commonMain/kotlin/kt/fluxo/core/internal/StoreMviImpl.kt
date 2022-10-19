@file:Suppress("GrazieInspection", "TooGenericExceptionCaught")

package kt.fluxo.core.internal

import kt.fluxo.core.IntentHandler
import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.dsl.StoreScope

@InternalFluxoApi
internal class StoreMviImpl<Intent, SideEffect : Any, State>(
    initialState: State,
    private val intentHandler: IntentHandler<Intent, State, SideEffect>,
    conf: FluxoConf<Intent, State, SideEffect>,
) : StoreBaseImpl<Intent, State, SideEffect>(
    initialState, conf,
) {
    override fun StoreScope<Intent, State, SideEffect>.handleIntent(intent: Intent) {
        with(intentHandler) {
            handleIntent(intent)
        }
    }
}
