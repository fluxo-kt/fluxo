package kt.fluxo.test

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

expect fun <T> runBlocking(coroutineContext: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T): T
