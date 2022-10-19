package kt.fluxo.core.internal

import kotlinx.coroutines.CoroutineScope
import kt.fluxo.core.StoreRequest
import kt.fluxo.core.dsl.FluxoInterceptorScope

internal class FluxoInterceptorScopeImpl<in Intent, in State>(
    override val storeName: String,
    private val storeScope: CoroutineScope,
    private val sendRequestToStore: suspend (StoreRequest<Intent, State>) -> Unit
) : FluxoInterceptorScope<Intent, State>,
    CoroutineScope by storeScope {

    override suspend fun sendToStore(queued: StoreRequest<Intent, State>) = sendRequestToStore(queued)
}
