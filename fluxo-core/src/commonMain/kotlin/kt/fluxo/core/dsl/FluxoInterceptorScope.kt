package kt.fluxo.core.dsl

import kotlinx.coroutines.CoroutineScope
import kt.fluxo.core.intercept.StoreRequest

public interface FluxoInterceptorScope<in Intent, in State> : CoroutineScope {

    public val storeName: String

    public suspend fun sendToStore(queued: StoreRequest<Intent, State>)
}
