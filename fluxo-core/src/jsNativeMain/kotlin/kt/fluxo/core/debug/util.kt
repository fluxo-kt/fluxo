package kt.fluxo.core.debug

import kt.fluxo.core.internal.FluxoIntent

/** No native implementation */
internal actual fun <S, SE : Any> debugIntentInfo(mvvmIntent: FluxoIntent<S, SE>): FluxoIntent<S, SE>? = null
