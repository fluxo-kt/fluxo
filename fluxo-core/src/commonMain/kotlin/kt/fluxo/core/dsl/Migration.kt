@file:Suppress(
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
    "KotlinRedundantDiagnosticSuppress",
    "NOTHING_TO_INLINE",
    "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE",
)

package kt.fluxo.core.dsl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kt.fluxo.common.annotation.InlineOnly
import kt.fluxo.core.Container
import kt.fluxo.core.FluxoIntent
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.Store

@InlineOnly
@Deprecated(message = "Please use container instead", replaceWith = ReplaceWith("container"))
public inline val <S, SE : Any> ContainerHost<S, SE>.store get() = container


@InlineOnly
@Deprecated(message = "Please use send instead", replaceWith = ReplaceWith("send(intent)"))
public suspend inline fun <S, SE : Any> Container<S, SE>.orbit(noinline intent: FluxoIntent<S, SE>) = emit(intent)

@InlineOnly
@Deprecated(message = "Please use send instead", replaceWith = ReplaceWith("send(intent)"))
public inline fun <I> Store<I, *>.accept(intent: I): Job = send(intent)


@InlineOnly
@OptIn(ExperimentalStdlibApi::class)
@Deprecated(message = "Please use intentContext instead", replaceWith = ReplaceWith("intentContext"))
public inline var FluxoSettings<*, *, *>.intentDispatcher: CoroutineDispatcher
    get() {
        return coroutineContext[CoroutineDispatcher]
            ?: scope?.run { coroutineContext[CoroutineDispatcher] }
            ?: Dispatchers.Default
    }
    set(value) {
        coroutineContext = value
    }
