package kt.fluxo.core.internal

import kotlinx.coroutines.CoroutineScope
import kt.fluxo.core.dsl.InterceptorScope
import kt.fluxo.core.intercept.StoreRequest

internal class InterceptorScopeImpl<in Intent, State>(
    override val storeName: String,
    private val sendRequest: suspend (StoreRequest<Intent, State>) -> Unit,
    coroutineScope: CoroutineScope,
) : InterceptorScope<Intent, State>, CoroutineScope by coroutineScope {

    override suspend fun postRequest(request: StoreRequest<Intent, State>) = sendRequest(request)
}
