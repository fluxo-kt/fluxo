package kt.fluxo.core.internal

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineExceptionHandler
import kt.fluxo.core.Bootstrapper
import kt.fluxo.core.FluxoInterceptor
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.IntentFilter
import kt.fluxo.core.annotation.InternalFluxoApi
import kotlin.coroutines.CoroutineContext

@PublishedApi
@InternalFluxoApi
@Suppress("LongParameterList")
internal class FluxoConf<Intent, State, SideEffect : Any>(
    val name: String,
    val lazy: Boolean,
    val closeOnExceptions: Boolean,
    val debugChecks: Boolean,
    val repeatOnSubscribedStopTimeout: Long,
    val sideEffectBufferSize: Int,
    val bootstrapper: Bootstrapper<Intent, State, SideEffect>?,
    val interceptors: Array<FluxoInterceptor<Intent, SideEffect, State>>,
    val intentFilter: IntentFilter<Intent, State>?,
    val inputStrategy: InputStrategy<Intent, State>,
    val eventLoopContext: CoroutineContext,
    val intentContext: CoroutineContext,
    val sideJobsContext: CoroutineContext,
    val interceptorContext: CoroutineContext,
    val exceptionHandler: CoroutineExceptionHandler?,
)

@PublishedApi
@InternalFluxoApi
internal fun <Intent, State, SideEffect : Any> FluxoSettings<Intent, State, SideEffect>.build(): FluxoConf<Intent, State, SideEffect> {
    return FluxoConf(
        name = name ?: "Store#${storeNumber.getAndIncrement()}>",
        lazy = lazy,
        closeOnExceptions = closeOnExceptions,
        debugChecks = debugChecks,
        repeatOnSubscribedStopTimeout = repeatOnSubscribedStopTimeout,
        sideEffectBufferSize = sideEffectBufferSize,
        bootstrapper = bootstrapper,
        interceptors = interceptors.toTypedArray(),
        intentFilter = intentFilter,
        inputStrategy = inputStrategy,
        eventLoopContext = eventLoopContext + context,
        intentContext = intentContext,
        sideJobsContext = sideJobsContext,
        interceptorContext = interceptorContext,
        exceptionHandler = exceptionHandler,
    )
}

private val storeNumber = atomic(0)
