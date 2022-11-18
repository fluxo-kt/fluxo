package kt.fluxo.test

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@ExperimentalCoroutinesApi
fun runUnitTest(
    context: CoroutineContext = EmptyCoroutineContext,
    dispatchTimeoutMs: Long = DEFAULT_DISPATCH_TIMEOUT_MS,
    testBody: suspend TestScope.() -> Unit,
): TestResult = runTest(context, dispatchTimeoutMs, testBody)

internal const val DEFAULT_DISPATCH_TIMEOUT_MS = 2_000L


suspend fun <T> inScope(
    scope: CoroutineScope,
    propagateCancellation: Boolean = false,
    block: suspend CoroutineScope.() -> T
): T? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        withContext(scope.coroutineContext, block = block)
    } catch (e: CancellationException) {
        if (propagateCancellation) {
            throw e
        }
        null
    }
}


inline fun <reified T : Throwable> mayFailWith(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    try {
        block()
    } catch (e: Throwable) {
        if (e !is T) {
            throw e
        }
    }
}
