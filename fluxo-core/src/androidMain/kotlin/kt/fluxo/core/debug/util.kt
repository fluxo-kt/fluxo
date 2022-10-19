package kt.fluxo.core.debug

import kt.fluxo.core.internal.FluxoIntent


// FIXME: Should use one from JVM!
/** No android implementation */
internal actual fun <S, SE : Any> debugIntentInfo(mvvmIntent: FluxoIntent<S, SE>): FluxoIntent<S, SE>? = null
