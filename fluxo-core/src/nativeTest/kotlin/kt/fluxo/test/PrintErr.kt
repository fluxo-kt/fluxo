package kt.fluxo.test

// https://stackoverflow.com/a/53134599/1816338
val STDERR = platform.posix.fdopen(2, "w")
internal actual fun printErr(message: Any?) {
    platform.posix.fprintf(STDERR, "%s\n", message.toString())
    platform.posix.fflush(STDERR)
}
