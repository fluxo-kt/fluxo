package kt.fluxo.tests

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kt.fluxo.core.FluxoIntent
import kt.fluxo.core.FluxoRuntimeException
import kt.fluxo.core.InputStrategy
import kt.fluxo.core.InputStrategy.InBox.Fifo
import kt.fluxo.core.InputStrategy.InBox.Lifo
import kt.fluxo.core.InputStrategy.InBox.Parallel
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.dsl.InputStrategyScope
import kt.fluxo.core.intent
import kt.fluxo.core.store
import kt.fluxo.core.updateState
import kt.fluxo.test.CoroutineScopeAwareTest
import kt.fluxo.test.IgnoreNativeAndJs
import kt.fluxo.test.KMM_PLATFORM
import kt.fluxo.test.Platform
import kt.fluxo.test.TestLoggingStoreFactory
import kt.fluxo.test.runUnitTest
import kt.fluxo.test.testLog
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Suppress("SuspendFunctionOnCoroutineScope")
internal class InputStrategyTest : CoroutineScopeAwareTest() {

    internal companion object {
        private const val DBG = 0

        private const val NUMBER_OF_ITEMS = 1000
    }

    /**
     * @TODO: Check for absence of undelivered intents for strategies that should always deliver and vice versa.
     */
    @Suppress("CyclomaticComplexMethod")
    private suspend fun CoroutineScope.input_strategy_test(
        strategy: InputStrategy,
        equal: Boolean = false,
        nonOrderedEqual: Boolean = false,
        generic: Boolean = false,
        parallel: Boolean = generic,
    ): List<Int> {
        val ints = IntArray(NUMBER_OF_ITEMS) { it }
        val lastValue = ints.last()

        val resLock = Mutex()
        val finishLock = Mutex(locked = true)

        val results = ArrayList<Int>(NUMBER_OF_ITEMS)
        val store = this.store(
            initialState = ints.first(),
            factory = if (DBG >= 3) TestLoggingStoreFactory() else null,
            handler = { intent: Int ->
                val resSize = resLock.withLock {
                    results.add(intent)
                    results.size
                }
                value = intent
                if (parallel) {
                    if (resSize == NUMBER_OF_ITEMS) {
                        finishLock.unlock()
                    }
                } else if (intent == lastValue) {
                    if (generic) delay(timeMillis = 25)
                    finishLock.unlock()
                }
            },
        ) { inputStrategy = strategy }
        if (DBG >= 1) testLog("Store created: $store")
        ints.forEach {
            if (it % 2 == 0) store.send(it) else store.emit(it)
        }

        // Wait till the processing finish
        if (DBG >= 1) testLog("All items sent, wait till the processing finish: $store")
        finishLock.withLock {}

        @Suppress("ComplexCondition")
        if ((generic || parallel) && (nonOrderedEqual || equal)) {
            if (DBG >= 1) testLog("Additional delay: $store")
            // TODO: Store.awaitIdle()
            while (results.size < NUMBER_OF_ITEMS) {
                delay(timeMillis = 10)
            }
        }
        if (DBG >= 1) testLog("Closing store: $store")
        store.closeAndWait()

        if (DBG >= 1) testLog("Store closed, start assertions: $store")
        if (equal || nonOrderedEqual) {
            assertEquals(NUMBER_OF_ITEMS, results.size, "unexpected results.size")
        }
        if (equal) {
            assertContentEquals(ints.asList(), results, "Expected to have all the results in the proper order")
        } else if (nonOrderedEqual) {
            assertEquals(NUMBER_OF_ITEMS, results.toHashSet().size, "results.toSet().size expected to be $NUMBER_OF_ITEMS")
            for (i in results.indices) {
                val num = results[i]
                assertTrue(num in ints, "$num not foound in results")
            }
        }

        return results
    }

    private fun t(timeoutMs: Long = 3_000, testBody: suspend TestScope.() -> Unit): TestResult =
        runUnitTest(dispatchTimeoutMs = timeoutMs, testBody = testBody)


    // region Fifo

    @Test
    fun fifo_test_scope() = t(timeoutMs = 5_000) { fifo_test() }

    @Test
    fun fifo_background_scope() = t { backgroundScope.fifo_test() }

    @Test
    @IgnoreNativeAndJs
    fun fifo_generic_scope() = t(timeoutMs = 20_000) { scope.fifo_test() }

    private suspend fun CoroutineScope.fifo_test() = input_strategy_test(strategy = Fifo, equal = true)

    // endregion


    // region Lifo

    @Test
    fun lifo_test_scope() = t { lifo_test() }

    @Test
    fun lifo_background_scope() = t { backgroundScope.lifo_test() }

    @Test
    fun lifo_generic_scope() = t { scope.lifo_test() }

    private suspend fun CoroutineScope.lifo_test(strategy: InputStrategy = Lifo) {
        @OptIn(ExperimentalStdlibApi::class)
        val isUnconfined = coroutineContext[CoroutineDispatcher] == Unconfined
        val results = input_strategy_test(strategy = strategy, equal = isUnconfined)
        assertEquals(NUMBER_OF_ITEMS - 1, results.last(), "Last result should be presented")
        if (!isUnconfined && KMM_PLATFORM != Platform.LINUX) {
            assertTrue(results.size < NUMBER_OF_ITEMS, "Expected to have cancelled intents with $strategy strategy")
        }
    }

    // endregion


    // region Parallel

    @Test
    fun parallel_test_scope() = t { parallel_test() }

    @Test
    fun parallel_background_scope() = t { backgroundScope.parallel_test() }

    @Test
    fun parallel_generic_scope() = t {
        scope.parallel_test(generic = true)
    }

    private suspend fun CoroutineScope.parallel_test(generic: Boolean = false) {
        @OptIn(ExperimentalStdlibApi::class)
        val isUnconfined = coroutineContext[CoroutineDispatcher] == Unconfined
        input_strategy_test(strategy = Parallel, equal = isUnconfined, nonOrderedEqual = true, generic = generic, parallel = true)
    }

    // endregion


    // region Custom input strategy

    @Test
    fun custom_test_scope() = t { custom_test() }

    @Test
    fun custom_background_scope() = t { backgroundScope.custom_test() }

    @Test
    fun custom_generic_scope() = t { scope.custom_test() }

    private suspend fun CoroutineScope.custom_test() = lifo_test(strategy = CustomInputStrategy())

    /** Custom strategy for testing customization mechanics. Works like 'Lifo'. */
    private class CustomInputStrategy : InputStrategy() {
        override fun toString() = "Custom"

        override fun <Request> createQueue(onUndeliveredElement: ((Request) -> Unit)?): Channel<Request> =
            Channel(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST, onUndeliveredElement = onUndeliveredElement)

        override suspend fun <Request> (InputStrategyScope<Request>).processRequests(queue: Flow<Request>) =
            queue.collectLatest(this)
    }

    // endregion


    /** @TODO: Return wnen InputStrategyGuardian will be ready */
    @Test
    @Ignore // Ignore while InputStrategyGuardian doesn't correctly verify the one-time only state access in parallel strategies.
    fun input_guardian_parallel_state_access_prevention() = t {
        val intent: FluxoIntent<Int, *> = intent@{
            assertTrue(value in 0..1)
            // Parallel input strategy requires that inputs only access or update the state at most once.
            assertFailsWith<FluxoRuntimeException> { value }
            sideJob(key = "${Random.nextLong()}") {
                // intent scope already closed.
                assertFailsWith<FluxoRuntimeException> { this@intent.noOp() }
                updateState { it + 1 }
            }
            // sideJob already declared
            assertFailsWith<FluxoRuntimeException> { noOp() }
        }
        val container = backgroundScope.container(0) {
            inputStrategy = Parallel
            debugChecks = true
            bootstrapper = intent
            optimized = false
            lazy = true
        }
        container.intent(intent)
        container.first { it == 2 }
        container.closeAndWait()
    }
}
