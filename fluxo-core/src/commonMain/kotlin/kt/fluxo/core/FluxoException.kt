package kt.fluxo.core

import kotlin.js.JsExport

@JsExport
public sealed interface FluxoException

@JsExport
public class FluxoClosedException
internal constructor(message: String, cause: Throwable?) : IllegalStateException(message, cause), FluxoException

@JsExport
public open class FluxoRuntimeException
internal constructor(message: String?, cause: Throwable?) : RuntimeException(message, cause), FluxoException
