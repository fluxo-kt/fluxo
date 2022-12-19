package kt.fluxo.core.dsl

import kotlinx.coroutines.Job
import kt.fluxo.core.annotation.FluxoDsl

@FluxoDsl
public interface BootstrapperScope<in Intent, State, in SideEffect : Any> : StoreScope<Intent, State, SideEffect> {

    public suspend fun postIntent(intent: Intent): Job
}
