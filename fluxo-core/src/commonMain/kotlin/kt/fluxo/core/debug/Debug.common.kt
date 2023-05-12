package kt.fluxo.core.debug

import kt.fluxo.common.annotation.InternalFluxoApi

@InternalFluxoApi
internal expect val DEBUG: Boolean

@InternalFluxoApi
internal expect fun <I> debugIntentWrapper(intent: I): I

@InternalFluxoApi
internal expect fun Any.debugClassName(): String?
