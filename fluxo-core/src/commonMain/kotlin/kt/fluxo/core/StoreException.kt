package kt.fluxo.core

public sealed interface StoreException

public class StoreClosedException
internal constructor(message: String, cause: Throwable?) : IllegalStateException(message, cause), StoreException

public open class StoreRuntimeException
internal constructor(message: String?, cause: Throwable?) : RuntimeException(message, cause), StoreException
