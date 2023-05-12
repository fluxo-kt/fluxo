package kt.fluxo.core.factory

import kt.fluxo.common.annotation.ExperimentalFluxoApi
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.IntentHandler
import kt.fluxo.core.internal.FluxoStore

@ExperimentalFluxoApi
public object FluxoStoreFactory : StoreFactory() {

    override fun <Intent, State, SideEffect : Any> createForDecoration(
        initialState: State,
        handler: IntentHandler<Intent, State, SideEffect>,
        settings: FluxoSettings<Intent, State, SideEffect>,
    ): StoreDecorator<Intent, State, SideEffect> = FluxoStore(
        initialState = initialState,
        intentHandler = handler,
        conf = settings,
    )
}
