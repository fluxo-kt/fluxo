package kt.fluxo.core.dsl

import kotlinx.coroutines.Deferred
import kt.fluxo.core.annotation.FluxoDsl

@FluxoDsl
public interface BootstrapperScope<in Intent, State, in SideEffect : Any> : StoreScope<Intent, State, SideEffect> {

    @Suppress("DeferredIsResult")
    public suspend fun postIntent(intent: Intent): Deferred<Unit>
}
