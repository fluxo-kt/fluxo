package kt.fluxo.core

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ChannelResult
import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.annotation.ThreadSafe
import kt.fluxo.core.internal.FluxoIntent
import kotlin.js.JsName

@ThreadSafe
public interface StoreMvvm<State, SideEffect : Any> : Store<FluxoIntent<State, SideEffect>, State, SideEffect> {

    @InternalFluxoApi
    override fun send(intent: FluxoIntent<State, SideEffect>): ChannelResult<Unit>

    @InternalFluxoApi
    override suspend fun sendAsync(intent: FluxoIntent<State, SideEffect>): Deferred<Unit>
}
