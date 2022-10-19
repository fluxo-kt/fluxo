package kt.fluxo.core

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kt.fluxo.core.annotation.ThreadSafe
import kotlin.js.JsName

@ThreadSafe
public interface Store<in Intent, out State, out SideEffect : Any> {

    public val name: String

    public val state: StateFlow<State>

    public val sideEffectFlow: Flow<SideEffect>


    @JsName("send")
    public fun send(intent: Intent): ChannelResult<Unit>

    @JsName("sendAndAwait")
    public suspend fun sendAsync(intent: Intent): Deferred<Unit>


    @JsName("start")
    public fun start()

    @JsName("stop")
    public fun stop()
}
