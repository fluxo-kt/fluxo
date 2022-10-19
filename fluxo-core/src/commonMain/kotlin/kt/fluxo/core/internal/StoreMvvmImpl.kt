package kt.fluxo.core.internal

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ChannelResult
import kt.fluxo.core.StoreMvvm
import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.debug.debugIntentInfo
import kt.fluxo.core.dsl.StoreScope

@PublishedApi
@InternalFluxoApi
internal class StoreMvvmImpl<State, SideEffect : Any>(
    initialState: State,
    conf: FluxoConf<FluxoIntent<State, SideEffect>, State, SideEffect>,
) : StoreBaseImpl<FluxoIntent<State, SideEffect>, State, SideEffect>(initialState, conf),
    StoreMvvm<State, SideEffect> {

    override fun send(intent: FluxoIntent<State, SideEffect>): ChannelResult<Unit> =
        super.send(intent = debugIntentInfo(intent) ?: intent)

    override suspend fun sendAsync(intent: FluxoIntent<State, SideEffect>): Deferred<Unit> =
        super.sendAsync(intent = debugIntentInfo(intent) ?: intent)

    override fun StoreScope<FluxoIntent<State, SideEffect>, State, SideEffect>.handleIntent(intent: FluxoIntent<State, SideEffect>) {
        intent()
    }
}
