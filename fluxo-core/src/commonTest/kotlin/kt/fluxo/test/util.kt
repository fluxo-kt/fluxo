package kt.fluxo.test

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
    val ms = time.nanosecond / 1_000_000
    println("$m:$s.$ms$thread $message")
}

private const val MAX_THREAD_INFO_LEN = 52
private val TIME_ZONE = TimeZone.currentSystemDefault()

private fun String.atLeast(size: Int, placeholder: Char = '0'): String {
    if (length >= size) return this
    return placeholder.toString().repeat(size - length) + this
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect fun threadInfo(): String?
