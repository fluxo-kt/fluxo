@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE", "DeferredIsResult")

package kt.fluxo.core.dsl

import kt.fluxo.core.Container
import kt.fluxo.core.ContainerHost
import kt.fluxo.core.FluxoIntent
import kt.fluxo.core.StoreHost
import kotlin.internal.InlineOnly

@InlineOnly
@Deprecated(message = "Please use the container instead", replaceWith = ReplaceWith("container"))
public val <State, SideEffect : Any> ContainerHost<State, SideEffect>.store get() = container

@InlineOnly
@Deprecated(message = "Please use the store instead", replaceWith = ReplaceWith("store"))
public val <Intent, State, SideEffect : Any> StoreHost<Intent, State, SideEffect>.container get() = store


@InlineOnly
@Deprecated(
    message = "Please use the send instead",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("sendAsync(intent)"),
)
public suspend fun <State, SideEffect : Any> Container<State, SideEffect>.orbit(intent: FluxoIntent<State, SideEffect>) = sendAsync(intent)
