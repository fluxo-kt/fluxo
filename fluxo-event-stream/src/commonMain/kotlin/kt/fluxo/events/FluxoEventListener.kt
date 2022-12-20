@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kotlin.internal.InlineOnly
import kotlin.jvm.JvmName

@ExperimentalFluxoApi
public interface FluxoEventListener<Intent, State, SideEffect : Any> {

    public fun CoroutineScope.start(events: Flow<FluxoEvent<Intent, State, SideEffect>>) {
        launch(start = CoroutineStart.UNDISPATCHED) {
            events.collect(::onNotify)
        }
    }

    public suspend fun onNotify(event: FluxoEvent<Intent, State, SideEffect>) {}
}

/**
 * Convenience factory for creating [FluxoEventListener] from a [function][onEvent].
 */
@ExperimentalFluxoApi
@Suppress("FunctionName")
@JvmName("create")
public inline fun <I, S, SE : Any> FluxoEventListener(
    crossinline onEvent: (event: FluxoEvent<I, S, SE>) -> Unit,
): FluxoEventListener<I, S, SE> {
    return object : FluxoEventListener<I, S, SE> {
        override suspend fun onNotify(event: FluxoEvent<I, S, SE>) = onEvent(event)
    }
}


/** [FluxoSettings.interceptors] convenience method */
@InlineOnly
@ExperimentalFluxoApi
public inline fun <I, S, SE : Any> FluxoSettings<I, S, SE>.interceptor(crossinline onEvent: (event: FluxoEvent<I, S, SE>) -> Unit) {
    // FIXME: interceptors.add(FluxoEventListener(onEvent))
}

/** [FluxoSettings.interceptors] convenience method */
@InlineOnly
@ExperimentalFluxoApi
public inline fun <I, S, SE : Any> FluxoSettings<I, S, SE>.onEvent(crossinline onEvent: (event: FluxoEvent<I, S, SE>) -> Unit) {
    interceptor(onEvent)
}
