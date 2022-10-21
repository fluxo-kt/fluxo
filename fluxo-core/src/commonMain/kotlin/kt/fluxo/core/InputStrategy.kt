package kt.fluxo.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kt.fluxo.core.dsl.InputStrategyScope
import kt.fluxo.core.intercept.StoreRequest

public abstract class InputStrategy<Intent, State> {

    public open fun createQueue(): Channel<StoreRequest<Intent, State>> {
        return Channel(Channel.BUFFERED, BufferOverflow.SUSPEND)
    }

    public open val parallelProcessing: Boolean get() = false

    public open val rollbackOnCancellation: Boolean get() = !parallelProcessing

    public abstract suspend fun InputStrategyScope<Intent, State>.processRequests(filteredQueue: Flow<StoreRequest<Intent, State>>)
}
