@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE")

package kt.fluxo.core.dsl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kt.fluxo.core.Container
import kt.fluxo.core.FluxoIntent
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.Store
import kotlin.internal.InlineOnly

@InlineOnly
@Deprecated(message = "Please use container instead", replaceWith = ReplaceWith("container"))
public inline val <S, SE : Any> ContainerHost<S, SE>.store get() = container


@InlineOnly
@Deprecated(
    message = "Please use send instead",
    replaceWith = ReplaceWith("send(intent)"),
    level = DeprecationLevel.ERROR,
)
public suspend inline fun <S, SE : Any> Container<S, SE>.orbit(noinline intent: FluxoIntent<S, SE>) = sendAsync(intent)

@InlineOnly
@Deprecated(
    message = "Please use send instead",
    replaceWith = ReplaceWith("send(intent)"),
    level = DeprecationLevel.WARNING,
)
public inline fun <I, S, SE : Any> Store<I, S, SE>.accept(intent: I) = send(intent)


@InlineOnly
@Deprecated(message = "Please use intentContext instead", replaceWith = ReplaceWith("intentContext"))
@OptIn(ExperimentalStdlibApi::class)
public inline var <I, S, SE : Any> FluxoSettings<I, S, SE>.intentDispatcher: CoroutineDispatcher
    get() = intentContext[CoroutineDispatcher] ?: Dispatchers.Default
    set(value) {
        intentContext = value
    }
