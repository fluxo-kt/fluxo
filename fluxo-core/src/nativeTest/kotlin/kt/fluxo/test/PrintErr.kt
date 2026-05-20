package kt.fluxo.test

import kotlinx.cinterop.ExperimentalForeignApi

// https://stackoverflow.com/a/53134599/1816338
@OptIn(ExperimentalForeignApi::class)
val STDERR = platform.posix.fdopen(2, "w")
@OptIn(ExperimentalForeignApi::class)
internal actual fun printErr(message: Any?) {
    platform.posix.fprintf(STDERR, "%s\n", message.toString())
    platform.posix.fflush(STDERR)
}
