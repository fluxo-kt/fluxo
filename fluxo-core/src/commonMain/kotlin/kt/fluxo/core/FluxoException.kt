package kt.fluxo.core

import kotlin.coroutines.cancellation.CancellationException
import kotlin.js.JsExport
import kotlin.jvm.JvmSynthetic

@JsExport
public sealed interface FluxoException

/**
 * Thrown when using a closed [Store].
 */
@JsExport
public class FluxoClosedException
internal constructor(message: String, @get:JvmSynthetic override val cause: Throwable?) : CancellationException(message), FluxoException

@JsExport
public open class FluxoRuntimeException
internal constructor(message: String?, cause: Throwable?) : RuntimeException(message, cause), FluxoException
