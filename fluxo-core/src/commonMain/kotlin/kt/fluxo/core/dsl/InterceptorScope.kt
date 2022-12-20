package kt.fluxo.core.dsl

import kotlinx.coroutines.CoroutineScope
import kt.fluxo.core.Store
import kt.fluxo.core.intercept.StoreRequest

public interface InterceptorScope<Intent, State, SideEffect : Any> : CoroutineScope {

    public val store: Store<Intent, State, SideEffect>

    public suspend fun postRequest(request: StoreRequest<Intent, State>)
}
