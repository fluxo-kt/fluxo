package kt.fluxo.test

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


fun testLog(message: Any?) {
    val time = Clock.System.now().toLocalDateTime(TIME_ZONE).time
    val thread = threadInfo().let {
        when {
            it.isNullOrEmpty() -> ""
            it.length < MAX_THREAD_INFO_LEN -> " [${it.atLeast(MAX_THREAD_INFO_LEN, ' ')}]"
            else -> " [${it.substring(it.length - MAX_THREAD_INFO_LEN, it.length)}]"
        }
    }
    val m = time.minute.toString().atLeast(2)
    val s = time.second.toString().atLeast(2)
    val ms = (time.nanosecond / 1_000_000).toString().atLeast(3)
    printErr("$m:$s.$ms$thread $message")
}

@Suppress("PrivatePropertyName")
private val MAX_THREAD_INFO_LEN = if (KMM_PLATFORM == Platform.JVM) 52 else 4

@Suppress("PrivatePropertyName")
private val TIME_ZONE: TimeZone = TimeZone.currentSystemDefault()

private fun String.atLeast(size: Int, placeholder: Char = '0'): String {
    if (length >= size) return this
    return placeholder.toString().repeat(size - length) + this
}

internal expect fun threadInfo(): String?

/** Prints the given [message] and the line separator to the standard error stream (if possiblem stdout otherwise). */
internal expect fun printErr(message: Any?)


operator fun <T> StateFlow<T>.getValue(thisRef: Any?, property: Any?): T = value

operator fun <T> MutableStateFlow<T>.setValue(thisRef: Any?, property: Any?, value: T) {
    this.value = value
}
