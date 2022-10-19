package kt.fluxo.core.internal

import kt.fluxo.core.annotation.InternalFluxoApi

@InternalFluxoApi
@Suppress("FunctionName")
internal expect fun <K, V> ConcurrentHashMap(): MutableMap<K, V>
