package kt.fluxo.core.debug

import kt.fluxo.common.annotation.ExperimentalFluxoApi
import kt.fluxo.core.Store
import kt.fluxo.core.factory.StoreDecorator
import kt.fluxo.core.factory.StoreDecoratorBase

/**
 * Debug features for [Store]
 *
 * @see debugIntentWrapper
 */
@ExperimentalFluxoApi
internal class DebugStoreDecorator<Intent, State, SideEffect : Any>(
    store: StoreDecorator<Intent, State, SideEffect>,
) : StoreDecoratorBase<Intent, State, SideEffect>(store) {
    override suspend fun emit(value: Intent) = super.emit(debugIntentWrapper(value))

    override fun send(intent: Intent) = super.send(debugIntentWrapper(intent))
}
