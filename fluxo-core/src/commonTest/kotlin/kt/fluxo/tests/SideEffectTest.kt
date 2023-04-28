package kt.fluxo.tests

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kt.fluxo.core.Container
import kt.fluxo.core.SideEffectStrategy
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.debug.debugClassName
import kt.fluxo.core.internal.Closeable
import kt.fluxo.test.IgnoreJs
import kt.fluxo.test.getValue
import kt.fluxo.test.runUnitTest
import kt.fluxo.test.setValue
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class SideEffectTest {
    private companion object {
        private val BASIC_STRATEGIES = arrayOf(SideEffectStrategy.RECEIVE, SideEffectStrategy.CONSUME)
    }

    // TODO: Timeout of 2000ms exceeded
    // js, node/browser
    // https://github.com/fluxo-kt/fluxo/actions/runs/3797488660/jobs/6458474871#step:8:897
    // jvm, android
    //  https://github.com/fluxo-kt/fluxo/actions/runs/4018550114/jobs/6904304918#step:8:375
    //  https://github.com/fluxo-kt/fluxo/actions/runs/4018550114/jobs/6904304918#step:8:597
    @Test
    fun side_effects_strategies_api() {
        assertEquals("RECEIVE", SideEffectStrategy.RECEIVE.toString())
        assertEquals("CONSUME", SideEffectStrategy.CONSUME.toString())
        assertEquals("DISABLE", SideEffectStrategy.DISABLE.toString())
        assertEquals("SHARE(replay=0)", SideEffectStrategy.SHARE().toString())

        for (s in arrayOf(
            SideEffectStrategy.RECEIVE,
            SideEffectStrategy.CONSUME,
            SideEffectStrategy.DISABLE,
            SideEffectStrategy.SHARE(),
        )) {
            assertEquals(s, s)
            assertEquals(s.hashCode(), s.hashCode())
            assertEquals(s.toString(), s.toString())
            assertSame(s, s)
        }

        assertEquals(SideEffectStrategy.SHARE(), SideEffectStrategy.SHARE())
        assertNotSame(SideEffectStrategy.SHARE(), SideEffectStrategy.SHARE())
        assertNotEquals(SideEffectStrategy.SHARE(1), SideEffectStrategy.SHARE())
        @Suppress("RemoveExplicitTypeArguments")
        assertNotEquals<SideEffectStrategy>(SideEffectStrategy.RECEIVE, SideEffectStrategy.SHARE())
    }


    @Test
    @IgnoreJs
    fun side_effects_are_emitted_ordered_by_default() = runUnitTest {
        for (strategy in BASIC_STRATEGIES) {
            // Uses Fifo strategy by default, saving order of intents
            val container = backgroundScope.container<Unit, Int>(Unit) {
                sideEffectStrategy = strategy
            }
            container.sideEffectFlow.test {
                val n = 500
                val post = backgroundScope.launch {
                    repeat(n) {
                        container.postSideEffect(it)
                        yield()
                    }
                }
                repeat(n) {
                    assertEquals(it, awaitItem())
                }
                post.join()
                container.close()
                awaitComplete()
            }
            container.closeAndWait()
        }
    }

    @Test
    fun side_effects_are_not_multicast() = runUnitTest {
        val container = backgroundScope.container<Unit, Int>(Unit)
        val result = MutableStateFlow<List<Int>>(listOf())
        val block: suspend CoroutineScope.() -> Unit = {
            container.sideEffectFlow.collect {
                do {
                    val prev = result.value
                    val next = result.value + it
                } while (!result.compareAndSet(prev, next))
            }
        }
        repeat(3) {
            backgroundScope.launch(block = block)
        }
        repeat(3) {
            container.postSideEffect(it)
        }
        val results = result.first { it.size == 3 }
        assertEquals(listOf(0, 1, 2), results)
        container.closeAndWait()
    }


    @Test
    fun side_effects_are_cached_when_there_are_no_subscribers__receive_strategy() =
        side_effects_are_cached_when_there_are_no_subscribers(SideEffectStrategy.RECEIVE)

    @Test
    fun side_effects_are_cached_when_there_are_no_subscribers__consume_strategy() =
        side_effects_are_cached_when_there_are_no_subscribers(SideEffectStrategy.CONSUME)

    private fun side_effects_are_cached_when_there_are_no_subscribers(strategy: SideEffectStrategy) = runUnitTest {
        val container = backgroundScope.container<Unit, Int>(Unit) {
            sideEffectStrategy = strategy
        }
        repeat(3) {
            container.postSideEffect(it)
        }
        assertContentEquals(listOf(0, 1, 2), container.sideEffectFlow.take(3).toList())
        container.closeAndWait()
    }


    @Test
    fun consumed_side_effects_are_not_resent() = runUnitTest {
        val container = backgroundScope.container<Unit, Int>(Unit)
        repeat(5) {
            container.postSideEffect(it)
        }
        assertContentEquals(listOf(0, 1, 2), container.sideEffectFlow.take(3).toList())
        assertContentEquals(listOf(3), container.sideEffectFlow.take(1).toList())
        container.close()

        assertContentEquals(listOf(4), container.sideEffectFlow.toList())
        container.closeAndWait()
    }

    // TODO: Timeout of 2000ms exceeded
    //  :jsNodeTest win CI
    //  https://github.com/fluxo-kt/fluxo/actions/runs/4795937502/jobs/8531106215#step:8:1158
    @Test
    @IgnoreJs
    fun only_new_side_effects_are_emitted_when_resubscribing() = runUnitTest {
        val container = backgroundScope.container<Unit, Int>(Unit)
        val flow = container.sideEffectFlow
        container.postSideEffect(123)
        assertContentEquals(listOf(123), flow.take(1).toList())

        backgroundScope.launch {
            repeat(1000) {
                container.postSideEffect(it)
            }
        }

        assertContentEquals(0..999, flow.take(1000).toList())
        container.closeAndWait()
    }

    @Test
    fun disabled_side_effects() = runUnitTest {
        for (strategy in BASIC_STRATEGIES + SideEffectStrategy.SHARE()) {
            val container = container(Unit) {
                // cover extra lines of code
                name = ""
                debugChecks = false
            }
            assertFailsWith<IllegalStateException> {
                (container as Container<*, *>).sideEffectFlow
            }
            container.closeAndWait()
        }
    }

    private suspend fun Container<Unit, Int>.postSideEffect(value: Int) = send(intent = {
        postSideEffect(sideEffect = value)
    })


    @Test
    fun side_effects_can_be_collected_only_once_with_consume_strategy() = runUnitTest {
        val container = container<Unit, Int>(Unit) {
            sideEffectStrategy = SideEffectStrategy.CONSUME
        }
        container.postSideEffect(1)
        val flow = container.sideEffectFlow
        assertContentEquals(listOf(1), flow.take(1).toList())
        assertFailsWith<IllegalStateException> { flow.take(1).toList() }
        container.postSideEffect(2)
        assertFailsWith<IllegalStateException> { flow.take(1).toList() }
        container.closeAndWait()
    }


    // TODO: Expected value to be true.
    // :linuxX64
    // :testReleaseUnit
    // https://github.com/fluxo-kt/fluxo/actions/runs/3713049727/jobs/6295263338#step:7:471
    // Expected to have closed effects
    // :testReleaseUnit
    // https://github.com/fluxo-kt/fluxo/actions/runs/3713762763/jobs/6296805652#step:6:332
    @Test
    fun unconsumed_side_effects_will_be_closed__consume_strategy() = unconsumed_side_effects_will_be_closed(SideEffectStrategy.CONSUME)

    @Test
    fun unconsumed_side_effects_will_be_closed__receive_strategy() = unconsumed_side_effects_will_be_closed(SideEffectStrategy.RECEIVE)

    private fun unconsumed_side_effects_will_be_closed(strategy: SideEffectStrategy) = runUnitTest {
        val container = container<Unit, Any>(initialState = Unit, setup = {
            sideEffectStrategy = strategy
            scope = CoroutineScope(SupervisorJob())
            debugChecks = false
            closeOnExceptions = false
            onError {}
        })
        val collect = backgroundScope.launch {
            container.sideEffectFlow.take(30).toList()
        }

        var hasCloses = false
        val closeable = object : Closeable {
            override fun close() {
                if (hasCloses) {
                    throw IllegalStateException()
                }
                hasCloses = true
            }
        }
        repeat(100) {
            container.send(intent = {
                postSideEffect(sideEffect = if (it % 2 == 0) it else closeable)
            })
        }

        collect.join()
        container.closeAndWait()
        assertTrue(hasCloses, "Expected to have closed effects (strategy: ${strategy.debugClassName()})")
    }


    // TODO: Timeout of 10000ms exceeded
    // js, node/browser
    // macosX64Background
    // jvm, testReleaseUnit
    // https://github.com/fluxo-kt/fluxo/actions/runs/3713049727/jobs/6295263119#step:6:593
    // https://github.com/fluxo-kt/fluxo/actions/runs/3713049727/jobs/6295263119#step:6:617
    // https://github.com/fluxo-kt/fluxo/actions/runs/3713367491/jobs/6295930135#step:6:563
    // https://github.com/fluxo-kt/fluxo/actions/runs/3713762763/jobs/6296805652#step:6:291
    // https://github.com/fluxo-kt/fluxo/actions/runs/3714168176/jobs/6297720613#step:6:271
    @Test
    @Ignore // TODO: Should be returned after `fluxo-event-stream` will be added
    fun undelivered_side_effects__consume_strategy() = undelivered_side_effects(SideEffectStrategy.CONSUME)

    @Test
    @Ignore // TODO: Should be returned after `fluxo-event-stream` will be added
    fun undelivered_side_effects__receive_strategy() = undelivered_side_effects(SideEffectStrategy.RECEIVE)

    private fun undelivered_side_effects(strategy: SideEffectStrategy) = runUnitTest {
        val container = backgroundScope.container<Unit, Int>(initialState = Unit, setup = {
            sideEffectStrategy = strategy
            sideEffectBufferSize = Channel.CONFLATED
        })
        var hasUndelivered by MutableStateFlow(false)
        val intercept = backgroundScope.launch {
            // FIXME:
            // container.eventsFlow.first { it is FluxoEvent.SideEffectUndelivered }
            // hasUndelivered = true
        }
        launch {
            val effects = container.sideEffectFlow.take(16).toList()
            assertTrue(effects.isNotEmpty(), "effects.size = ${effects.size} (strategy: ${strategy.debugClassName()})")
        }
        repeat(200) {
            container.postSideEffect(it)
        }
        intercept.join()
        assertTrue(hasUndelivered, "Expected to have undelivered side effects (strategy: ${strategy.debugClassName()})")
        container.closeAndWait()
    }
}
