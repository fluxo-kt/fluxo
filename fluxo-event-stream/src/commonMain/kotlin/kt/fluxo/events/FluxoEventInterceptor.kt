package kt.fluxo.events

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.transformWhile
import kt.fluxo.core.Bootstrapper
import kt.fluxo.core.SideJob
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.dsl.InterceptorScope
import kt.fluxo.core.intercept.FluxoInterceptor
import kt.fluxo.core.intercept.StoreRequest
import kotlin.coroutines.CoroutineContext

// FIXME: Tests for rewrite, and retry anything

/**
 * Provides store events as coroutine [Flow][rawFlow].
 */
@ExperimentalFluxoApi
public class FluxoEventInterceptor<I, S, SE : Any>(
    private val flow: MutableSharedFlow<FluxoEvent<I, S, SE>> = MutableSharedFlow(extraBufferCapacity = 256),
    @Suppress("UNCHECKED_CAST")
    private val listeners: Array<out FluxoEventListener<I, S, SE>> = EMPTY_LISTENERS as Array<out FluxoEventListener<I, S, SE>>,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : FluxoInterceptor<I, S, SE>, FluxoEventSource<I, S, SE> {

    private companion object {
        private val EMPTY_LISTENERS: Array<out FluxoEventListener<*, *, *>> = arrayOf()
    }

    @ExperimentalFluxoApi
    @Suppress("MemberVisibilityCanBePrivate")
    public val rawFlow: SharedFlow<FluxoEvent<I, S, SE>> = flow

    override val eventsFlow: Flow<FluxoEvent<I, S, SE>> = flow.transformWhile {
        emit(it)
        it !is FluxoEvent.StoreClosed
    }


    init {
        // launch interceptors handling (stop on StoreClosed)
        if (listeners.isNotEmpty()) {
            val scope = CoroutineScope(coroutineContext)
            for (i in listeners.indices) {
                with(listeners[i]) {
                    scope.start(eventsFlow)
                }
            }
        }
    }


    public override suspend fun InterceptorScope<I, S, SE>.started() {
        flow.emit(FluxoEvent.StoreStarted(store))
    }

    public override suspend fun InterceptorScope<I, S, SE>.closed(cause: Throwable?) {
        flow.emit(FluxoEvent.StoreClosed(store, cause))
    }

    public override fun InterceptorScope<I, S, SE>.unhandledError(error: Throwable) {
        flow.tryEmit(FluxoEvent.UnhandledError(store, error))
    }


    public override suspend fun InterceptorScope<I, S, SE>.stateChanged(state: S) {
        flow.tryEmit(FluxoEvent.StateChanged(store, state))
    }


    public override suspend fun <B : Bootstrapper<I, S, SE>> InterceptorScope<I, S, SE>.bootstrap(
        bootstrapper: B,
        proceed: suspend (bootstrapper: B) -> Unit,
    ) {
        flow.emit(FluxoEvent.BootstrapperStarted(store, bootstrapper))
        try {
            proceed(bootstrapper)
            flow.emit(FluxoEvent.BootstrapperCompleted(store, bootstrapper))
        } catch (ce: CancellationException) {
            flow.emit(FluxoEvent.BootstrapperCancelled(store, bootstrapper, ce))
            throw ce
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            flow.emit(FluxoEvent.BootstrapperError(store, bootstrapper, e))
            throw e
        }
    }


    public override suspend fun InterceptorScope<I, S, SE>.intentQueue(intent: I, proceed: suspend (intent: I) -> Unit) {
        flow.tryEmit(FluxoEvent.IntentQueued(store, intent))
    }

    public override suspend fun <R : StoreRequest.HandleIntent<I, S>> InterceptorScope<I, S, SE>.intent(
        request: R,
        proceed: suspend (request: R) -> Unit,
    ) {
        try {
            flow.emit(FluxoEvent.IntentAccepted(store, request.intent))
            proceed(request)
            flow.emit(FluxoEvent.IntentHandled(store, request.intent))
        } catch (ce: CancellationException) {
            flow.emit(FluxoEvent.IntentCancelled(store, request.intent, ce))
            throw ce
        } catch (e: Throwable) {
            flow.emit(FluxoEvent.IntentError(store, request.intent, e))
            throw e
        }
    }

    public override suspend fun InterceptorScope<I, S, SE>.intentUndelivered(intent: I) {
        flow.tryEmit(FluxoEvent.IntentUndelivered(store, intent))
    }


    public override suspend fun InterceptorScope<I, S, SE>.sideEffect(sideEffect: SE, proceed: suspend (sideEffect: SE) -> Unit) {
        try {
            proceed(sideEffect)
            flow.emit(FluxoEvent.SideEffectEmitted(store, sideEffect))
        } catch (ce: CancellationException) {
            flow.emit(FluxoEvent.SideEffectUndelivered(store, sideEffect))
            throw ce
        }
    }

    public override suspend fun InterceptorScope<I, S, SE>.sideEffectUndelivered(sideEffect: SE) {
        flow.tryEmit(FluxoEvent.SideEffectUndelivered(store, sideEffect))
    }


    public override suspend fun <SJ : SideJob<I, S, SE>> InterceptorScope<I, S, SE>.sideJobQueue(key: String, sideJob: SJ) {
        flow.tryEmit(FluxoEvent.SideJobQueued(store, key, sideJob))
    }

    public override suspend fun <SJ : SideJob<I, S, SE>> InterceptorScope<I, S, SE>.sideJob(
        key: String,
        sideJob: SJ,
        proceed: suspend (SJ) -> Unit,
    ) {
        try {
            flow.emit(FluxoEvent.SideJobStarted(store, key, sideJob))
            proceed(sideJob)
            flow.emit(FluxoEvent.SideJobCompleted(store, key, sideJob))
        } catch (ce: CancellationException) {
            flow.emit(FluxoEvent.SideJobCancelled(store, key, sideJob, ce))
            throw ce
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            flow.emit(FluxoEvent.SideJobError(store, key, sideJob, e))
            throw e
        }
    }
}
