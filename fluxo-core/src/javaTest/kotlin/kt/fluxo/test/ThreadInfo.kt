package kt.fluxo.test

internal actual fun threadInfo(): String? = Thread.currentThread().run { "$name *$id" }
