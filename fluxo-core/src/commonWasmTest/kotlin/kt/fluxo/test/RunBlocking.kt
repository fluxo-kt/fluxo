package kt.fluxo.test

import kotlin.coroutines.CoroutineContext

/** [runBlocking] is not available on Wasm. */
actual fun <T> runBlocking(coroutineContext: CoroutineContext, block: suspend () -> T): T {
    throw NotImplementedError("runBlocking is not available on Wasm")
}
