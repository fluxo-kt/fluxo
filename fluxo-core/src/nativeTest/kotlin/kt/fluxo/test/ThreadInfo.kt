package kt.fluxo.test

import platform.posix.pthread_self

internal actual fun threadInfo(): String? = "#${pthread_self()}"
