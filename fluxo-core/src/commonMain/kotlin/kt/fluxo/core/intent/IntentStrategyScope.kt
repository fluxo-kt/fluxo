package kt.fluxo.core.intent

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kt.fluxo.core.annotation.CallSuper
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.factory.StoreDecorator
import kotlin.coroutines.CoroutineContext
import kotlin.js.JsName

/**
 * Special subview of the [StoreScope] and [StoreDecorator]
 *
 * @see StoreScope
 */
@ExperimentalFluxoApi
public interface IntentStrategyScope<in Intent, State> : CoroutineScope {

    /** @see StoreScope.value */
    public var value: State

    /**
     * Callback to directly execute the [Intent].
     */
    @CallSuper
    @JsName("executeIntent")
    public suspend fun executeIntent(intent: Intent)

    /**
     *
     * **Note:** method name should be different from the one in [StoreDecorator]
     *
     * @see StoreDecorator.onUndeliveredIntent
     */
    @CallSuper
    @JsName("undeliveredIntent")
    public fun undeliveredIntent(intent: Intent, wasResent: Boolean)

    /**
     * Handles uncaught [exception] in the given [context].
     *
     * @see CoroutineExceptionHandler.handleException
     */
    @CallSuper
    @JsName("handleException")
    public fun handleException(context: CoroutineContext, exception: Throwable)
}
