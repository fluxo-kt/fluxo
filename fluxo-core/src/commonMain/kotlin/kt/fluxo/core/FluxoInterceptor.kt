package kt.fluxo.core

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kt.fluxo.core.dsl.FluxoInterceptorScope

public interface FluxoInterceptor<Intent, SideEffect : Any, State> {

    public fun FluxoInterceptorScope<Intent, State>.start(events: Flow<FluxoEvent<Intent, State, SideEffect>>) {
        launch(start = CoroutineStart.UNDISPATCHED) {
            events.collect(::onNotify)
        }
    }

    public suspend fun onNotify(event: FluxoEvent<Intent, State, SideEffect>) {}
}
