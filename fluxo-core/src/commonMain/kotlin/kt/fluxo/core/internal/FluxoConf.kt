package kt.fluxo.core.internal

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineExceptionHandler
import kt.fluxo.core.Bootstrapper
import kt.fluxo.core.FluxoInterceptor
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.IntentFilter
import kt.fluxo.core.SideEffectsStrategy
import kt.fluxo.core.annotation.InternalFluxoApi
import kotlin.coroutines.CoroutineContext

@PublishedApi
@InternalFluxoApi
@Suppress("LongParameterList")
internal class FluxoConf<Intent, State, SideEffect : Any>
internal constructor(
    internal val name: String,
    internal val lazy: Boolean,
    internal val closeOnExceptions: Boolean,
    internal val debugChecks: Boolean,
    internal val repeatOnSubscribedStopTimeout: Long,
    internal val sideEffectBufferSize: Int,
    internal val bootstrapper: Bootstrapper<in Intent, State, SideEffect>?,
    internal val interceptors: Array<out FluxoInterceptor<Intent, State, SideEffect>>,
    internal val intentFilter: IntentFilter<in Intent, State>?,
    internal val inputStrategy: InputStrategy,
    internal val sideEffectsStrategy: SideEffectsStrategy,
    internal val eventLoopContext: CoroutineContext,
    internal val intentContext: CoroutineContext,
    internal val sideJobsContext: CoroutineContext,
    internal val interceptorContext: CoroutineContext,
    internal val exceptionHandler: CoroutineExceptionHandler?,
)

@PublishedApi
@InternalFluxoApi
internal fun <Intent, State, SideEffect : Any> FluxoSettings<Intent, State, SideEffect>.build(): FluxoConf<Intent, State, SideEffect> {
    return FluxoConf(
        name = name ?: "store#${storeNumber.getAndIncrement()}",
        lazy = lazy,
        closeOnExceptions = closeOnExceptions,
        debugChecks = debugChecks,
        repeatOnSubscribedStopTimeout = repeatOnSubscribedStopTimeout,
        sideEffectBufferSize = sideEffectBufferSize,
        bootstrapper = bootstrapper,
        interceptors = interceptors.toTypedArray(),
        intentFilter = intentFilter,
        inputStrategy = inputStrategy,
        sideEffectsStrategy = sideEffectsStrategy,
        eventLoopContext = eventLoopContext + context,
        intentContext = intentContext,
        sideJobsContext = sideJobsContext,
        interceptorContext = interceptorContext,
        exceptionHandler = exceptionHandler,
    )
}

private val storeNumber = atomic(0)
