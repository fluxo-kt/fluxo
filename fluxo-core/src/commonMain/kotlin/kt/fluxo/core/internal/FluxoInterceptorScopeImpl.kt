package kt.fluxo.core.internal

import kotlinx.coroutines.CoroutineScope
import kt.fluxo.core.dsl.FluxoInterceptorScope
import kt.fluxo.core.intercept.StoreRequest

internal class FluxoInterceptorScopeImpl<in Intent, in State>(
    override val storeName: String,
    private val storeScope: CoroutineScope,
    private val sendRequestToStore: suspend (StoreRequest<Intent, State>) -> Unit
) : FluxoInterceptorScope<Intent, State>,
    CoroutineScope by storeScope {

    override suspend fun sendToStore(queued: StoreRequest<Intent, State>) = sendRequestToStore(queued)
}
