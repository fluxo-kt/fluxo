package kt.fluxo.core.debug

import kt.fluxo.core.annotation.InternalFluxoApi
import kt.fluxo.core.internal.FluxoIntent

@InternalFluxoApi
internal expect fun <S, SE : Any> debugIntentInfo(mvvmIntent: FluxoIntent<S, SE>): FluxoIntent<S, SE>?
