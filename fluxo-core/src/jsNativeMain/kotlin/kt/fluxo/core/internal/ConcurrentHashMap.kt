package kt.fluxo.core.internal

@Suppress("FunctionName")
internal actual fun <K, V> ConcurrentHashMap(): MutableMap<K, V> = HashMap()
