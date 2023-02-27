@file:Suppress("Filename")

package kt.fluxo.test

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val DEBUG = false

/**
 * [runTest] with lowered default [timeout][dispatchTimeoutMs] and more safety.
 *
 * * [dispatchTimeoutMs] is 2 seconds by default
 * * Catches suppressed exceptions (can be helpful when debugging some problems)
 *
 * See issue [#3270](https://github.com/Kotlin/kotlinx.coroutines/issues/3270) for more details.
 */
@ExperimentalCoroutinesApi
@Suppress("NestedBlockDepth")
fun runUnitTest(
    context: CoroutineContext = EmptyCoroutineContext,
    dispatchTimeoutMs: Long = 2_000L,
    testBody: suspend TestScope.() -> Unit,
): TestResult {
    if (DEBUG) {
        printStackItemWithTestName()
    }
    var scope: TestScope? = null
    try {
        return runTest(context, dispatchTimeoutMs) {
            scope = this
            val testJob = coroutineContext[Job]!!

            if (DEBUG) {
                testJob.invokeOnCompletion {
                    testLog("Test finish")
                }
            }

            var job: Job? = null
            if (KMM_PLATFORM != Platform.JS) {
                val s = this
                job = backgroundScope.launch(Dispatchers.Default) {
                    // NOTE: delay is safe bacause of Dispatcher
                    delay(dispatchTimeoutMs)
                    if (isActive && testJob.isActive) {
                        val message = "Timed out waiting for $dispatchTimeoutMs ms"
                        s.cancel(CancellationException(message))
                        testLog(message)
                    }
                }
            }

            try {
                testBody()
            } catch (e: CancellationException) {
                job?.cancel(e)
                throw e
            } finally {
                job?.cancel()
            }
        }
    } catch (e: IllegalStateException) {
        val s = scope
        // Internal TestScope "check" failed, for example, because of unfinished child jobs.
        // In such cases, it may lose cancellation exceptions.
        if (s != null && e.message == "Check failed.") {
            for (sc in arrayOf(s, s.backgroundScope)) {
                try {
                    @OptIn(InternalCoroutinesApi::class)
                    sc.coroutineContext[Job]
                        ?.getCancellationException()
                        ?.let(e::addSuppressed)
                } catch (_: IllegalStateException) {
                }
            }
            // TODO: TestScopeImpl has an uncaughtExceptions field that can be read here, at least in JVM
        }
        throw e
    }
}

private fun printStackItemWithTestName() {
    try {
        val stack = Throwable().stackTraceToString().lineSequence()
            .filter { it.isNotBlank() }
        var line = ""
        var found = false
        for (l in stack) {
            line = l
            when {
                ::runUnitTest.name in l -> found = true
                found -> break
            }
        }
        testLog(line.trim())
    } catch (e: Throwable) {
        @Suppress("PrintStackTrace")
        testLog("printStackItemWithTestName error: $e\n${e.printStackTrace()}")
    }
}


suspend fun <T> inScope(scope: CoroutineScope, propagateCancellation: Boolean = false, block: suspend CoroutineScope.() -> T): T? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        withContext(scope.coroutineContext, block = block)
    } catch (ce: CancellationException) {
        if (propagateCancellation) {
            throw ce
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
