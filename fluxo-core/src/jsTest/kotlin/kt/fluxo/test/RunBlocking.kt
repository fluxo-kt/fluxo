package kt.fluxo.test

import kotlin.coroutines.CoroutineContext

/** [runBlocking] isn't available in JS! */
actual fun <T> runBlocking(coroutineContext: CoroutineContext, block: suspend () -> T): T {
    // https://discuss.kotlinlang.org/t/coroutines-how-to-bridge-blocking-and-non-blocking-code/12390
    // https://youtrack.jetbrains.com/issue/KT-29403
    throw NotImplementedError("runBlocking isn't available in JS")
}
