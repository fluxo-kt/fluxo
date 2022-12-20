package kt.fluxo.core.intercept

import kt.fluxo.core.Bootstrapper
import kt.fluxo.core.SideJob
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.intercept.StoreRequest.HandleIntent
import kt.fluxo.core.dsl.InterceptorScope as Scope

@ExperimentalFluxoApi
public interface FluxoInterceptor<I, S, SE : Any> {
    public suspend fun Scope<I, S, SE>.started() {}

    public suspend fun Scope<I, S, SE>.closed(cause: Throwable?) {}

    public fun Scope<I, S, SE>.unhandledError(error: Throwable) {}


    public suspend fun Scope<I, S, SE>.stateChanged(state: S) {}


    public suspend fun <B : Bootstrapper<I, S, SE>> Scope<I, S, SE>.bootstrap(bootstrapper: B, proceed: suspend (bootstrapper: B) -> Unit) {
    }


    public suspend fun Scope<I, S, SE>.intentQueue(intent: I, proceed: suspend (intent: I) -> Unit) {}

    public suspend fun <R : HandleIntent<I, S>> Scope<I, S, SE>.intent(request: R, proceed: suspend (request: R) -> Unit) {}

    public suspend fun Scope<I, S, SE>.intentUndelivered(intent: I) {}


    public suspend fun Scope<I, S, SE>.sideEffect(sideEffect: SE, proceed: suspend (sideEffect: SE) -> Unit) {}

    public suspend fun Scope<I, S, SE>.sideEffectUndelivered(sideEffect: SE) {}


    public suspend fun <SJ : SideJob<I, S, SE>> Scope<I, S, SE>.sideJobQueue(key: String, sideJob: SJ) {}

    public suspend fun <SJ : SideJob<I, S, SE>> Scope<I, S, SE>.sideJob(key: String, sideJob: SJ, proceed: suspend (SJ) -> Unit) {}
}
