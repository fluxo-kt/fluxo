package kt.fluxo.core.internal


public class StoreClosedException
internal constructor(message: String, cause: Throwable?) : IllegalStateException(message, cause)
