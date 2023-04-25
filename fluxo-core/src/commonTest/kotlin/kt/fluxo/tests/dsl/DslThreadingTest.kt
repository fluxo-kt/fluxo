package kt.fluxo.tests.dsl

import app.cash.turbine.test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.dsl.ContainerHost
import kt.fluxo.core.intent
import kt.fluxo.core.intent.IntentStrategy
import kt.fluxo.core.intent.IntentStrategy.InBox.ChannelLifo
import kt.fluxo.core.intent.IntentStrategy.InBox.Direct
import kt.fluxo.core.intent.IntentStrategy.InBox.Fifo
import kt.fluxo.core.intent.IntentStrategy.InBox.Lifo
import kt.fluxo.core.intent.IntentStrategy.InBox.Parallel
import kt.fluxo.core.send
import kt.fluxo.test.IgnoreJs
import kt.fluxo.test.KMM_PLATFORM
import kt.fluxo.test.Platform
import kt.fluxo.test.TestLoggingStoreFactory
import kt.fluxo.test.runUnitTest
import kt.fluxo.test.testLog
import kt.fluxo.tests.IntentStrategyTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DslThreadingTest {

    @Test
    @IgnoreJs // Doesn't work in JS; TODO: Shouldn't some combinations work?
    fun blocking_intent_with_context_switch_does_not_block_the_reducer() = test(
        strategies = { scopeName ->
            arrayOf(
                Parallel,
                Lifo,
                ChannelLifo(ordered = false),
                ChannelLifo(ordered = true),
                Fifo.takeIf { "Dispatchers.Default" !in scopeName },
                Direct.takeIf { "Dispatchers.Default" !in scopeName },
            )
        }
    ) { middleware, _, info ->
        if (DBG > 0) testLog("backgroundIntent")
        val bgJob = middleware.backgroundIntent()
        middleware.waitForMutexUnlock()
        middleware.assertReducerAndComplete(info, bgJob)
    }

    // TODO: Timeout of 2000ms exceeded
    //  js, node
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4756275550/jobs/8451589421#step:8:1260
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4759165968/jobs/8458124296#step:8:1239
    //  https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4759202632/jobs/8458210085#step:8:1222
    @Test
    @IgnoreJs
    fun suspending_intent_does_not_block_the_reducer() = test(
        scopes = {
            if (KMM_PLATFORM != Platform.JS) ALL_SCOPES() else mapOf(
                "TestScope" to this,
                "BackgroundTestScope" to backgroundScope,
            )
        },
        strategies = {
            // TODO: Shouldn't some other strategies work in JS too?
            if (KMM_PLATFORM != Platform.JS) ALL_STRATEGIES else arrayOf(Parallel)
        }
    ) { middleware, _, info ->
        if (DBG > 0) testLog("suspendingIntent")
        val suspendingJob = middleware.suspendingIntent()
        middleware.waitForMutexUnlock()
        middleware.assertReducerAndComplete(info, suspendingJob)
    }


    // TODO: Error: Timeout of 2000ms exceeded.
    //  js, node (local, mac)
    //   https://github.com/fluxo-kt/fluxo-mvi/actions/runs/4018550114/jobs/6904304918#step:8:1216
    @Test
    @IgnoreJs // Shouldn't work in JS because of the busy-waiting in the only thread.
    fun blocking_intent_without_context_switch_does_not_block_the_reducer() = test(
        scopes = {
            mapOf(
                "TestScope + Dispatchers.Default" to (this + Default),
                "BackgroundTestScope + Dispatchers.Default" to (backgroundScope + Default),
                // TestScope variants are single-threaded and completely blocked with busy-waiting.
                // "Unconfined" and "NonParallel" dispatchers are blocked with busy-waiting.
            )
        },
        strategies = {
            arrayOf(
                Parallel, Fifo, Lifo,
                ChannelLifo(ordered = false),
                // Can be blocked with real busy-waiting, but here it supports cooperative cancellation.
                ChannelLifo(ordered = true),
                // "Undispatched" strategy blocked with busy-waiting
                //Direct,
            )
        }
    ) { middleware, strategy, info ->
        if (DBG > 0) testLog("blockingIntent")
        val blockingJob = middleware.blockingIntent()
        middleware.waitForMutexUnlock()

        blockingJob.cancelForFifo(strategy)
        middleware.assertReducerAndComplete(info, blockingJob)
    }


    @Test
    @IgnoreJs // Shouldn't work in JS because of the busy-waiting in the only thread.
    fun blocking_reducer_does_not_block_an_intent() = test(
        scopes = {
            mapOf(
                "TestScope + Dispatchers.Default" to (this + Default),
                "BackgroundTestScope + Dispatchers.Default" to (backgroundScope + Default),
                // TestScope variants are single-threaded and completely blocked with busy-waiting.
                // "Unconfined" and "NonParallel" dispatchers are blocked with busy-waiting.
            )
        },
        strategies = {
            arrayOf(
                Parallel, Fifo, Lifo,
                ChannelLifo(ordered = false),
                // Can be blocked with real busy-waiting, but here it supports cooperative cancellation.
                ChannelLifo(ordered = true),
                // "Undispatched" strategy blocked with busy-waiting
                //Direct,
            )
        },
    ) { middleware, strategy, info ->
        if (DBG > 0) testLog("blockingReducer")
        val blockingJob = middleware.blockingReducer()
        middleware.waitForMutexUnlock(mutex = middleware.reducerMutex)

        blockingJob.cancelForFifo(strategy)
        if (DBG > 0) testLog("simpleIntent")
        middleware.simpleIntent()
        middleware.waitForMutexUnlock()

        middleware.assertReducerAndComplete(info, blockingJob)
    }


    private data class TestState(val id: Int)

    private fun test(
        scopes: TestScope.() -> Map<String, CoroutineScope?> = ALL_SCOPES,
        strategies: TestScope.(scopeName: String) -> Array<out IntentStrategy.Factory?> = { ALL_STRATEGIES },
        block: suspend TestScope.(middleware: BaseDslMiddleware, strategy: IntentStrategy.Factory, info: String) -> Unit,
    ) = runUnitTest(timeoutMs = TEST_TIMEOUT) {
        repeat(TEST_REPETITIONS) { iteration ->
            for ((scopeName, scope) in scopes()) {
                scope ?: continue
                for (strategy in strategies(scopeName)) {
                    strategy ?: continue
                    val info = "#$iteration, strategy: $strategy; scope: $scopeName; platform: $KMM_PLATFORM"
                    if (DBG > 0) testLog(info)

                    val middleware = BaseDslMiddleware(scope, strategy)
                    try {
                        block(middleware, strategy, info)
                    } catch (e: AssertionError) {
                        if (info !in (e.message ?: "")) {
                            throw IllegalStateException("$e // $info", e)
                        }
                        throw e
                    }

                    if (DBG > 0) testLog("complete\n")
                }
            }
        }
    }

    private fun Job.cancelForFifo(strategy: IntentStrategy.Factory) {
        // Otherwise, Fifo will be blocked
        if (strategy == Fifo) {
            if (DBG > 0) testLog("blocking job cancellation for $strategy")
            cancel()
            assertFalse(isActive, "blocking job was not cancelled for $strategy")
        }
    }

    private suspend fun BaseDslMiddleware.assertReducerAndComplete(info: String, job: Job? = null, action: Int = Random.nextInt()) {
        val middleware = this
        container.test {
            if (DBG > 0) testLog("containerReducerAssertions")
            assertEquals(INIT_VALUE, middleware.container.value, info)
            assertEquals(INIT_VALUE, awaitItem(), info)
            middleware.reducer(action)
            assertEquals(TestState(action), awaitItem(), info)
            assertEquals(TestState(action), middleware.container.value, info)
        }
        assertTrue(middleware.container.isActive, info)
        middleware.container.closeAndWait()
        job?.let {
            assertFalse(it.isActive, info)
        }
    }

    @Suppress("ControlFlowWithEmptyBody", "EmptyWhileBlock", "DEPRECATION")
    private inner class BaseDslMiddleware(
        scope: CoroutineScope,
        strategy: IntentStrategy.Factory = Parallel,
        debugChecks: Boolean = true,
    ) : ContainerHost<TestState, String> {

        override val container = scope.container<TestState, String>(
            initialState = INIT_VALUE,
            factory = if (DBG >= 3) TestLoggingStoreFactory() else null,
        ) {
            this.intentStrategy = strategy
            this.debugChecks = debugChecks
        }

        // kotlinx.coroutines.sync.Mutex isn't encouraged to be used by JetBrains team.
        //  https://github.com/Kotlin/kotlinx.coroutines/issues/87#issuecomment-381067399
        //  https://github.com/Kotlin/kotlinx.coroutines/issues/1382#issuecomment-517916422
        private val intentMutex: Any = if (Random.nextInt() % 2 == 0) MutableStateFlow(true) else Mutex(locked = true)

        val reducerMutex = Mutex(locked = true)

        suspend fun waitForMutexUnlock(mutex: Any = intentMutex) {
            if (DBG > 0) testLog("waitForMutexUnlock")
            withContext(Default) {
                // NOTE: withTimeout is safe because of Dispatcher
                withTimeout(UNLOCK_TIMEOUT) {
                    when (mutex) {
                        is Mutex -> if (mutex.isLocked) {
                            val owner = this
                            mutex.withLock(owner) {
                                assertTrue(mutex.isLocked, "Mutex.isLocked = false when locked (M)")
                                assertTrue(mutex.holdsLock(owner), "Mutex.holdsLock() = false (M)")
                            }
                            assertFalse(mutex.isLocked, "Mutex.isLocked = false (M)")
                        }

                        else -> {
                            @Suppress("UNCHECKED_CAST")
                            val m = mutex as StateFlow<Boolean>
                            if (m.value) {
                                assertFalse(m.first { !it }, "Mutex waiting (SF)") // wait for unlock
                            }
                            assertFalse(m.value, "Mutex value check (SF)")
                        }
                    }
                }
            }
        }

        private fun Any.doUnlock() {
            when (this) {
                is Mutex -> {
                    assertTrue(isLocked, "doUnlock called for non-locked Mutex (M)")
                    unlock()
                }

                else -> {
                    @Suppress("UNCHECKED_CAST")
                    val m = this as MutableStateFlow<Boolean>
                    assertTrue(m.compareAndSet(expect = true, update = false), "doUnlock called for non-locked Mutex (SF)")
                }
            }
            if (DBG > 0) testLog("unlocked")
        }

        fun reducer(action: Int) = intent {
            reduce { state ->
                state.copy(id = action)
            }
        }

        fun blockingReducer() = send {
            if (DBG > 0) testLog("blockingReducer start")
            val context = currentCoroutineContext()
            reduce {
                reducerMutex.doUnlock()
                if (DBG > 0) testLog("blockingReducer busy work")
                while (context.isActive) {
                }
                if (DBG >= 2) testLog("blockingReducer completed, throw CancellationException")
                // cancel reducer without applying a new state
                throw CancellationException("ignore")
            }
        }

        fun backgroundIntent() = send {
            intentMutex.doUnlock()
            withContext(Default) {
                while (currentCoroutineContext().isActive) {
                }
            }
            if (DBG >= 2) testLog("backgroundIntent completed")
        }

        fun blockingIntent() = send {
            intentMutex.doUnlock()
            while (currentCoroutineContext().isActive) {
            }
            if (DBG >= 2) testLog("blockingIntent completed")
        }

        fun suspendingIntent() = send {
            intentMutex.doUnlock()
            withContext(Default) {
                // NOTE: delay is safe because of Dispatcher
                delay(Int.MAX_VALUE.toLong())
            }
        }

        fun simpleIntent() = intent {
            intentMutex.doUnlock()
            noOp()
        }
    }


    private companion object {
        private inline val NonParallelDispatcher get() = IntentStrategyTest.NonParallelDispatcher

        private val ALL_SCOPES: TestScope.() -> Map<String, CoroutineScope?> = {
            mapOf(
                "TestScope" to this,
                "BackgroundTestScope" to backgroundScope,

                "TestScope + Dispatchers.Default" to (this + Default),
                "BackgroundTestScope + Dispatchers.Default" to (backgroundScope + Default),

                "TestScope + Dispatchers.Unconfined" to (this + Unconfined),
                "BackgroundTestScope + Dispatchers.Unconfined" to (backgroundScope + Unconfined),

                "TestScope + NonParallelDispatcher" to (this + NonParallelDispatcher),
                "BackgroundTestScope + NonParallelDispatcher" to (backgroundScope + NonParallelDispatcher),
            )
        }

        private inline val ALL_STRATEGIES get() = IntentStrategyTest.ALL_STRATEGIES

        /** Some problems are stochastic, try to detect them with repetitions */
        private val TEST_REPETITIONS = if (KMM_PLATFORM != Platform.JS) 2 else 100

        private val TEST_TIMEOUT = 3_000L * TEST_REPETITIONS.coerceAtMost(20)

        /** Lock awaiting timeout in milliseconds */
        private const val UNLOCK_TIMEOUT = 5000L

        private val INIT_VALUE = TestState(42)

        private const val DBG = 0
    }
}
