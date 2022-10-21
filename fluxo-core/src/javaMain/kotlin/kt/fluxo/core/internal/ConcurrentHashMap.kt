package kt.fluxo.core.internal

import java.util.concurrent.ConcurrentHashMap as CHM

@Suppress("FunctionName")
internal actual fun <K, V> ConcurrentHashMap(): MutableMap<K, V> = CHM()
