package kt.fluxo.test

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect fun <T> runBlocking(coroutineContext: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T): T
