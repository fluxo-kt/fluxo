package kt.fluxo.core

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kt.fluxo.core.dsl.FluxoInterceptorScope
import kt.fluxo.core.intercept.FluxoEvent
import kotlin.coroutines.EmptyCoroutineContext

public interface FluxoInterceptor<Intent, State, SideEffect : Any> {

    public fun FluxoInterceptorScope<Intent, State>.start(events: Flow<FluxoEvent<Intent, State, SideEffect>>) {
        val context = when (coroutineContext[CoroutineName]) {
            null -> EmptyCoroutineContext
            else -> CoroutineName("$storeName: interceptor ${this::class.simpleName}")
        }
        launch(context = context, start = CoroutineStart.UNDISPATCHED) {
            events.collect(::onNotify)
        }
    }

    public suspend fun onNotify(event: FluxoEvent<Intent, State, SideEffect>) {}
}

@Suppress("FunctionName")
public inline fun <Intent, State, SideEffect : Any> FluxoInterceptor(
    crossinline onNotify: (event: FluxoEvent<Intent, State, SideEffect>) -> Unit,
): FluxoInterceptor<Intent, State, SideEffect> {
    return object : FluxoInterceptor<Intent, State, SideEffect> {
        override suspend fun onNotify(event: FluxoEvent<Intent, State, SideEffect>) = onNotify(event)
    }
}
