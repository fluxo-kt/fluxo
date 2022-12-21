package kt.fluxo.events

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kotlin.coroutines.CoroutineContext

@ExperimentalFluxoApi
public interface FluxoEventSource<I, S, SE : Any> {

    public val eventsFlow: Flow<FluxoEvent<I, S, SE>>

    public fun addListener(listener: FluxoEventListener<I, S, SE>, coroutineContext: CoroutineContext = Dispatchers.Unconfined)
}
