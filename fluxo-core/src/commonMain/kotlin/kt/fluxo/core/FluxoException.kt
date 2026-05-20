package kt.fluxo.core

import kotlin.coroutines.cancellation.CancellationException
import kt.fluxo.common.annotation.FluxoJsExport

@FluxoJsExport
public sealed interface FluxoException

/**
 * Thrown when using a closed [Store].
 */
@FluxoJsExport
public class FluxoClosedException
internal constructor(message: String, override val cause: Throwable?) : CancellationException(message), FluxoException

@FluxoJsExport
public open class FluxoRuntimeException
internal constructor(message: String?, cause: Throwable?) : RuntimeException(message, cause), FluxoException
