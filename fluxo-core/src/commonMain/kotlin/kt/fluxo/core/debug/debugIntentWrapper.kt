package kt.fluxo.core.debug

import kt.fluxo.core.annotation.InternalFluxoApi

@InternalFluxoApi
internal expect fun <I> debugIntentWrapper(intent: I): I
