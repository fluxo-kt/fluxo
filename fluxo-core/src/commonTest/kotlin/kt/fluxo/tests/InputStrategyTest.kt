package kt.fluxo.tests

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kt.fluxo.core.FluxoIntent
import kt.fluxo.core.FluxoRuntimeException
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.input.ChannelBasedInputStrategy
import kt.fluxo.core.input.InputStrategy
import kt.fluxo.core.input.InputStrategy.InBox.ChannelLifo
import kt.fluxo.core.input.InputStrategy.InBox.Direct
import kt.fluxo.core.input.InputStrategy.InBox.Fifo
import kt.fluxo.core.input.InputStrategy.InBox.Lifo
import kt.fluxo.core.input.InputStrategy.InBox.Parallel
import kt.fluxo.core.input.InputStrategyScope
import kt.fluxo.core.intent
import kt.fluxo.core.store
import kt.fluxo.core.updateState
import kt.fluxo.test.CoroutineScopeAwareTest
import kt.fluxo.test.DEFAULT_TEST_TIMEOUT_MS
import kt.fluxo.test.IgnoreJs
import kt.fluxo.test.IgnoreNativeAndJs
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

        internal val ALL_STRATEGIES = arrayOf(
            Parallel,
            Direct,
            Fifo,
            Lifo,
            ChannelLifo(ordered = true),
            ChannelLifo(ordered = false),
            CustomStrategy, // Lifo-like custom strategy
        )

        internal val NonParallelDispatcher = Default.limitedParallelism(1)
    }

    /**
     * @TODO: Check for absence of undelivered intents for strategies that should always deliver and vice versa.
     */
    @Suppress("CyclomaticComplexMethod")
    private suspend fun CoroutineScope.input_strategy_test(
        strategy: InputStrategy.Factory,
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

    private fun t(timeoutMs: Long = DEFAULT_TEST_TIMEOUT_MS, testBody: suspend TestScope.() -> Unit): TestResult =
        runUnitTest(timeoutMs = timeoutMs, testBody = testBody)


    // region Fifo

    @Test
    fun fifo_test_scope() = t(timeoutMs = 5_000) { fifo_test() }

    @Test
    fun fifo_background_scope() = t { backgroundScope.fifo_test() }

    @Test
    fun fifo_test_scope_plus_default_dispatcher() = t { (this + Default).fifo_test() }

    @Test
    fun fifo_background_scope_plus_default_dispatcher() = t { (backgroundScope + Default).fifo_test() }

    @Test
    fun fifo_test_scope_plus_unconfined_dispatcher() = t { (this + Unconfined).fifo_test() }

    @Test
    fun fifo_background_scope_plus_unconfined_dispatcher() = t { (backgroundScope + Unconfined).fifo_test() }

    @Test
    fun fifo_test_scope_plus_non_parallel_dispatcher() = t { (this + NonParallelDispatcher).fifo_test() }

    @Test
    fun fifo_background_scope_plus_non_parallel_dispatcher() = t { (backgroundScope + NonParallelDispatcher).fifo_test() }

    @Test
    @IgnoreNativeAndJs // test can freeze on Native and JS targets
    fun fifo_generic_scope() = t(timeoutMs = 20_000) { scope.fifo_test() }

    private suspend fun CoroutineScope.fifo_test() = input_strategy_test(strategy = Fifo, equal = true)

    // endregion


    // region Lifo

    @Test
    fun lifo_test_scope() = t { lifo_test() }

    @Test
    @IgnoreJs // TODO: Can we fix it for JS?
    fun lifo_background_scope() = t { backgroundScope.lifo_test() }

    @Test
    @IgnoreJs // TODO: Can we fix it for JS?
    fun lifo_test_scope_plus_default_dispatcher() = t { (this + Default).lifo_test() }

    // TODO: Failed with "Last result should be presented. Expected <999>, actual <867>" :linuxX64Background
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4774084419/jobs/8487545269#step:8:1139
    @Test
    @IgnoreJs // TODO: Can we fix it for JS?
    fun lifo_background_scope_plus_default_dispatcher() = t { (backgroundScope + Default).lifo_test() }

    @Test
    @IgnoreJs // TODO: Can we fix it for JS?
    fun lifo_test_scope_plus_unconfined_dispatcher() = t { (this + Unconfined).lifo_test() }

    @Test
    @IgnoreJs // TODO: Can we fix it for JS?
    fun lifo_background_scope_plus_unconfined_dispatcher() = t { (backgroundScope + Unconfined).lifo_test() }

    @Test
    @IgnoreJs // TODO: Can we fix it for JS?
    fun lifo_test_scope_plus_non_parallel_dispatcher() = t { (this + NonParallelDispatcher).lifo_test() }

    @Test
    @IgnoreJs // TODO: Can we fix it for JS?
    fun lifo_background_scope_plus_non_parallel_dispatcher() = t { (backgroundScope + NonParallelDispatcher).lifo_test() }

    @Test
    fun lifo_generic_scope() = t { scope.lifo_test() }

    private suspend fun CoroutineScope.lifo_test() = lifo_test(strategy = Lifo)

    private suspend fun CoroutineScope.lifo_test(strategy: InputStrategy.Factory) {
        @OptIn(ExperimentalStdlibApi::class)
        val isUnconfined = coroutineContext[CoroutineDispatcher] == Unconfined
        val results = input_strategy_test(strategy = strategy, equal = isUnconfined)
        val last = NUMBER_OF_ITEMS - 1
        assertTrue(last in results, "Last result should be presented ($last), but wasn't in ${results.size} results")
        if (isUnconfined) {
            return
        }
        assertTrue(results.size in 1..NUMBER_OF_ITEMS, "Can have cancelled intents with $strategy strategy")
    }

    // endregion


    // region ChannelLifo(ordered = true)

    @Test
    fun ordered_lifo_test_scope() = t { ordered_lifo_test() }

    @Test
    fun ordered_lifo_background_scope() = t { backgroundScope.ordered_lifo_test() }

    // TODO: AssertionError: Last result should be presented. Expected <999>, actual <991>.
    //  :macosX64BackgroundTest
    //   https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4067858739/jobs/7005714035#step:8:1109
    @Test
    @IgnoreJs // TODO: Can we fix it for JS?
    fun ordered_lifo_test_scope_plus_default_dispatcher() = t { (this + Default).ordered_lifo_test() }

    @Test
    @IgnoreJs // TODO: Can we fix it for JS?
    fun ordered_lifo_background_scope_plus_default_dispatcher() = t { (backgroundScope + Default).ordered_lifo_test() }

    // TODO: Timeout of 2000ms exceeded (js, node, win)
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4780106427/jobs/8497620699#step:8:1164
    @Test
    @IgnoreJs
    fun ordered_lifo_test_scope_plus_unconfined_dispatcher() = t { (this + Unconfined).ordered_lifo_test() }

    // TODO: Timeout of 2000ms exceeded (js, node, win)
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4796400068/jobs/8532119942#step:8:1219
    @Test
    @IgnoreJs
    fun ordered_lifo_background_scope_plus_unconfined_dispatcher() = t { (backgroundScope + Unconfined).ordered_lifo_test() }

    @Test
    fun ordered_lifo_test_scope_plus_non_parallel_dispatcher() = t { (this + NonParallelDispatcher).ordered_lifo_test() }

    @Test
    fun ordered_lifo_background_scope_plus_non_parallel_dispatcher() = t { (backgroundScope + NonParallelDispatcher).ordered_lifo_test() }

    @Test
    fun ordered_lifo_generic_scope() = t { scope.ordered_lifo_test() }

    private suspend fun CoroutineScope.ordered_lifo_test() = lifo_test(strategy = ChannelLifo(ordered = true))

    // endregion


    // region ChannelLifo(ordered = false)

    @Test
    fun channel_lifo_test_scope() = t { channel_lifo_test() }

    @Test
    fun channel_lifo_background_scope() = t { backgroundScope.channel_lifo_test() }

    @Test
    fun channel_lifo_test_scope_plus_default_dispatcher() = t { (this + Default).channel_lifo_test() }

    @Test
    fun channel_lifo_background_scope_plus_default_dispatcher() = t { (backgroundScope + Default).channel_lifo_test() }

    // TODO: AssertionError: Last result should be presented. Expected <999>, actual <987>.
    //  linuxX64
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4773710515/jobs/8486952812#step:10:750
    @Test
    @IgnoreJs // TODO: Can we fix it for JS?
    fun channel_lifo_test_scope_plus_unconfined_dispatcher() = t { (this + Unconfined).channel_lifo_test() }

    // TODO: Timeout of 2000ms exceeded
    //  js, node
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4759165968/jobs/8458124296#step:8:1223
    @Test
    @IgnoreJs // TODO: Can we fix it for JS?
    fun channel_lifo_background_scope_plus_unconfined_dispatcher() = t { (backgroundScope + Unconfined).channel_lifo_test() }

    @Test
    fun channel_lifo_test_scope_plus_non_parallel_dispatcher() = t { (this + NonParallelDispatcher).channel_lifo_test() }

    @Test
    fun channel_lifo_background_scope_plus_non_parallel_dispatcher() = t { (backgroundScope + NonParallelDispatcher).channel_lifo_test() }

    @Test
    fun channel_lifo_generic_scope() = t { scope.channel_lifo_test() }

    private suspend fun CoroutineScope.channel_lifo_test() = lifo_test(strategy = ChannelLifo(ordered = false))

    // endregion


    // region Parallel

    @Test
    fun parallel_test_scope() = t { parallel_test() }

    @Test
    fun parallel_background_scope() = t { backgroundScope.parallel_test() }

    @Test
    fun parallel_test_scope_plus_default_dispatcher() = t { (this + Default).parallel_test() }

    @Test
    fun parallel_background_scope_plus_default_dispatcher() = t { (backgroundScope + Default).parallel_test() }

    @Test
    fun parallel_test_scope_plus_unconfined_dispatcher() = t { (this + Unconfined).parallel_test() }

    @Test
    fun parallel_background_scope_plus_unconfined_dispatcher() = t { (backgroundScope + Unconfined).parallel_test() }

    @Test
    fun parallel_test_scope_plus_non_parallel_dispatcher() = t { (this + NonParallelDispatcher).parallel_test() }

    @Test
    fun parallel_background_scope_plus_non_parallel_dispatcher() = t { (backgroundScope + NonParallelDispatcher).parallel_test() }

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


    // region Direct

    @Test
    fun direct_test_scope() = t { direct_test() }

    @Test
    fun direct_background_scope() = t { backgroundScope.direct_test() }

    @Test
    fun direct_test_scope_plus_default_dispatcher() = t { (this + Default).direct_test() }

    @Test
    fun direct_background_scope_plus_default_dispatcher() = t { (backgroundScope + Default).direct_test() }

    @Test
    fun direct_test_scope_plus_unconfined_dispatcher() = t { (this + Unconfined).direct_test() }

    @Test
    fun direct_background_scope_plus_unconfined_dispatcher() = t { (backgroundScope + Unconfined).direct_test() }

    @Test
    fun direct_test_scope_plus_non_parallel_dispatcher() = t { (this + NonParallelDispatcher).direct_test() }

    @Test
    fun direct_background_scope_plus_non_parallel_dispatcher() = t { (backgroundScope + NonParallelDispatcher).direct_test() }

    @Test
    fun direct_generic_scope() = t { scope.direct_test() }

    private suspend fun CoroutineScope.direct_test() = input_strategy_test(strategy = Direct, equal = true)

    // endregion


    // region Custom input strategy
    // TODO: Test CustomFifoStrategy Fifo with resending and BufferOverflow.DROP_LATEST

    @Test
    fun custom_test_scope() = t { custom_test() }

    @Test
    fun custom_background_scope() = t { backgroundScope.custom_test() }

    @Test
    fun custom_test_scope_plus_default_dispatcher() = t { (this + Default).custom_test() }

    @Test
    fun custom_background_scope_plus_default_dispatcher() = t { (backgroundScope + Default).custom_test() }

    @Test
    fun custom_test_scope_plus_unconfined_dispatcher() = t { (this + Unconfined).custom_test() }

    // TODO: Timeout of 2000ms exceeded.
    //  :jsNodeTest local
    @Test
    fun custom_background_scope_plus_unconfined_dispatcher() = t { (backgroundScope + Unconfined).custom_test() }

    // TODO: Flaky test?
    @Test
    fun custom_test_scope_plus_non_parallel_dispatcher() = t { (this + NonParallelDispatcher).custom_test() }

    // TODO: Flaky test?
    @Test
    fun custom_background_scope_plus_non_parallel_dispatcher() = t { (backgroundScope + NonParallelDispatcher).custom_test() }

    @Test
    fun custom_generic_scope() = t { scope.custom_test() }

    private suspend fun CoroutineScope.custom_test() = lifo_test(strategy = CustomStrategy)

    /** Custom strategy for testing customization mechanics. Works like 'Lifo'. */
    private object CustomStrategy : InputStrategy.Factory {
        override fun toString() = "Custom"

        override fun <Intent, State> invoke(scope: InputStrategyScope<Intent, State>): InputStrategy<Intent, State> =
            CustomInputStrategy(scope)

        @Suppress("JS_FAKE_NAME_CLASH")
        private class CustomInputStrategy<Intent, State>(handler: InputStrategyScope<Intent, State>) :
            ChannelBasedInputStrategy<Intent, State>(handler, resendUndelivered = false) {
            override fun <Request> createQueue(onUndeliveredElement: ((Request) -> Unit)?): Channel<Request> =
                Channel(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST, onUndeliveredElement = onUndeliveredElement)

            override suspend fun launch() {
                requestsChannel.consumeAsFlow().collectLatest(::emit)
            }
        }
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
