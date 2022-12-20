package kt.fluxo.events

import kotlinx.coroutines.flow.Flow
import kt.fluxo.core.annotation.ExperimentalFluxoApi

@ExperimentalFluxoApi
public interface FluxoEventSource<I, S, SE : Any> {

    public val eventsFlow: Flow<FluxoEvent<I, S, SE>>
}
