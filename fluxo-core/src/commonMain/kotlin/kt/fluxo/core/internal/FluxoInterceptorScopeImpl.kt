package kt.fluxo.core.internal

import kotlinx.coroutines.CoroutineScope
import kt.fluxo.core.dsl.FluxoInterceptorScope
import kt.fluxo.core.intercept.StoreRequest

internal class FluxoInterceptorScopeImpl<in Intent, in State>(
    override val storeName: String,
    private val scope: CoroutineScope,
    private val sendRequest: suspend (StoreRequest<Intent, State>) -> Unit
) : FluxoInterceptorScope<Intent, State>, CoroutineScope by scope {

    override suspend fun postRequest(request: StoreRequest<Intent, State>) = sendRequest(request)
}
