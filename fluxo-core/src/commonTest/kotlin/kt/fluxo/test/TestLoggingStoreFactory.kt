package kt.fluxo.test

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kt.fluxo.core.Bootstrapper
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.IntentHandler
import kt.fluxo.core.SideJob
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.factory.FluxoStoreFactory
import kt.fluxo.core.factory.StoreDecorator
import kt.fluxo.core.factory.StoreDecoratorBase
import kt.fluxo.core.factory.StoreFactory
import kotlin.coroutines.CoroutineContext

@ExperimentalFluxoApi
class TestLoggingStoreFactory(
    private val delegate: StoreFactory = FluxoStoreFactory,
) : StoreFactory() {

    override fun <Intent, State, SideEffect : Any> createForDecoration(
        initialState: State,
        handler: IntentHandler<Intent, State, SideEffect>,
        settings: FluxoSettings<Intent, State, SideEffect>,
    ): StoreDecorator<Intent, State, SideEffect> {
        return TestLoggingStoreDecorator(delegate.createForDecoration(initialState, handler, settings))
    }

    private class TestLoggingStoreDecorator<Intent, State, SideEffect : Any>(
        store: StoreDecorator<Intent, State, SideEffect>,
    ) : StoreDecoratorBase<Intent, State, SideEffect>(store) {

        init {
            testLog("[$name] onStoreInit")
        }

        override suspend fun onBootstrap(bootstrapper: Bootstrapper<Intent, State, SideEffect>) {
            testLog("[$name] onBootstrapStarted: $bootstrapper")
            try {
                super.onBootstrap(bootstrapper)
                testLog("[$name] onBootstrapCompleted: $bootstrapper")
            } catch (ce: CancellationException) {
                testLog("[$name] onBootstrapCancelled: $bootstrapper // $ce")
                throw ce
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                testLog("[$name] onBootstrapError: $bootstrapper // $e")
                throw e
            }
        }

        override suspend fun onStart(bootstrapper: Bootstrapper<Intent, State, SideEffect>?) {
            testLog("[$name] onStoreStartBeginning")
            try {
                super.onStart(bootstrapper)
                testLog("[$name] onStoreStarted")
            } catch (ce: CancellationException) {
                testLog("[$name] onStoreStartedCancelled: $ce")
                throw ce
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                testLog("[$name] onStoreStartedError: $e")
                throw e
            }
        }

        override fun onStateChanged(state: State, fromState: State) {
            testLog("[$name] onStateChanged: $fromState => $state")
        }


        override suspend fun emit(value: Intent) {
            testLog("[$name] onIntentQueuing(emit): $value")
            super.emit(value)
        }

        override fun send(intent: Intent): Job {
            testLog("[$name] onIntentQueuing(send): $intent")
            return super.send(intent)
        }

        override suspend fun onIntent(intent: Intent) {
            testLog("[$name] onIntentAccepted: $intent")
            try {
                super.onIntent(intent)
                testLog("[$name] onIntentHandled: $intent")
            } catch (ce: CancellationException) {
                testLog("[$name] onIntentCancelled: $intent // $ce")
                throw ce
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                testLog("[$name] onIntentError: $intent // $e")
                throw e
            }
        }

        override fun onUndeliveredIntent(intent: Intent, wasResent: Boolean) {
            testLog("[$name] onUndeliveredIntent(wasResent=$wasResent): $intent")
            super.onUndeliveredIntent(intent = intent, wasResent = wasResent)
        }


        override suspend fun postSideEffect(sideEffect: SideEffect) {
            testLog("[$name] onSideEffectQueuing: $sideEffect")
            super.postSideEffect(sideEffect)
        }

        override fun onUndeliveredSideEffect(sideEffect: SideEffect, wasResent: Boolean) {
            testLog("[$name] onUndeliveredSideEffect(wasResent=$wasResent): $sideEffect")
            super.onUndeliveredSideEffect(sideEffect = sideEffect, wasResent = wasResent)
        }


        override suspend fun sideJob(
            key: String,
            context: CoroutineContext,
            start: CoroutineStart,
            onError: ((error: Throwable) -> Unit)?,
            block: SideJob<Intent, State, SideEffect>,
        ): Job {
            testLog("[$name] onSideJobQueuing: $key")
            return super.sideJob(key = key, context = context, start = start, onError = onError, block = block)
        }

        override suspend fun onSideJob(key: String, wasRestarted: Boolean, sideJob: SideJob<Intent, State, SideEffect>) {
            try {
                testLog("[$name] onSideJobStarted: $key")
                super.onSideJob(key, wasRestarted, sideJob)
                testLog("[$name] onSideJobCompleted: $key")
            } catch (ce: CancellationException) {
                testLog("[$name] onSideJobCancelled: $key // $ce")
                throw ce
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                testLog("[$name] onSideJobError: $key // $e")
                throw e
            }
        }


        override fun onUnhandledError(error: Throwable): Boolean {
            testLog("[$name] onUnhandledError: $error")
            return super.onUnhandledError(error)
        }

        override fun onClosed(cause: Throwable?) {
            testLog("[$name] onStoreClosed: $cause")
            super.onClosed(cause)
        }
    }
}
