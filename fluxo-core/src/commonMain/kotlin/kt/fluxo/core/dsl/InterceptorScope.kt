package kt.fluxo.core.dsl

import kotlinx.coroutines.CoroutineScope
import kt.fluxo.core.intercept.StoreRequest

public interface InterceptorScope<in Intent, State> : CoroutineScope {

    public val storeName: String

    public suspend fun postRequest(request: StoreRequest<Intent, State>)
}
