@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core.factory

import kt.fluxo.core.Bootstrapper
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.SideJob
import kt.fluxo.core.annotation.CallSuper
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.annotation.ThreadSafe
import kt.fluxo.core.dsl.StoreScope
import kotlin.js.JsName

/**
 * Use [StoreDecoratorBase] for convenience implementation
 *
 * @see StoreDecoratorBase
 */
@ThreadSafe
@ExperimentalFluxoApi
public interface StoreDecorator<Intent, State, SideEffect : Any> : StoreScope<Intent, State, SideEffect> {

    @CallSuper
    @JsName("init")
    @ExperimentalFluxoApi
    public fun init(decorator: StoreDecorator<Intent, State, SideEffect>, conf: FluxoSettings<Intent, State, SideEffect>)


    @CallSuper
    @JsName("onBootstrap")
    public suspend fun onBootstrap(bootstrapper: Bootstrapper<Intent, State, SideEffect>)

    @CallSuper
    @JsName("onStart")
    public suspend fun onStart(bootstrapper: Bootstrapper<Intent, State, SideEffect>?)


    @CallSuper
    @JsName("onStateChanged")
    public fun onStateChanged(state: State, fromState: State) {
    }


    @CallSuper
    @JsName("onIntent")
    public suspend fun onIntent(intent: Intent)

    @CallSuper
    @JsName("onSideJob")
    public suspend fun onSideJob(key: String, wasRestarted: Boolean, sideJob: SideJob<Intent, State, SideEffect>)


    @CallSuper
    @JsName("onUndeliveredIntent")
    public fun onUndeliveredIntent(intent: Intent, wasResent: Boolean) {
    }

    @CallSuper
    @JsName("onUndeliveredSideEffect")
    public fun onUndeliveredSideEffect(sideEffect: SideEffect, wasResent: Boolean) {
    }


    /**
     *
     * @return `true` if error was handled
     */
    @CallSuper
    @JsName("onUnhandledError")
    public fun onUnhandledError(error: Throwable): Boolean = false

    @CallSuper
    @JsName("onClosed")
    public fun onClosed(cause: Throwable?) {
    }
}
