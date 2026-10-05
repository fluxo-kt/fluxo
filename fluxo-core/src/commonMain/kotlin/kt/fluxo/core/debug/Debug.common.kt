package kt.fluxo.core.debug

import kt.fluxo.common.annotation.InternalFluxoApi

@InternalFluxoApi
internal expect val DEBUG: Boolean

@InternalFluxoApi
internal expect fun <I> debugIntentWrapper(intent: I): I

@InternalFluxoApi
internal expect fun Any.debugClassName(): String?

/**
 * Label for [intent] in debug coroutine names and error messages.
 *
 * Never calls `toString()` on a compiler-generated lambda: on JVM with kotlin-reflect on the classpath, that renders
 * the lambda signature through full reflection, which takes hundreds of milliseconds on first use (seconds on a busy
 * machine), and debug checks name a coroutine for every intent.
 */
@InternalFluxoApi
internal expect fun debugIntentLabel(intent: Any?): String
