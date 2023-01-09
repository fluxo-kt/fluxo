package kt.fluxo.core.dsl

import kotlinx.coroutines.Job
import kt.fluxo.core.annotation.FluxoDsl
import kotlin.js.JsName

@FluxoDsl
public interface BootstrapperScope<in Intent, State, in SideEffect : Any> : StoreScope<Intent, State, SideEffect> {

    @JsName("emit")
    public suspend fun emit(value: Intent)

    @JsName("send")
    public fun send(intent: Intent): Job
}
