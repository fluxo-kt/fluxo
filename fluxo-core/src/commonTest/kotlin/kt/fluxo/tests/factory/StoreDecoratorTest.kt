package kt.fluxo.tests.factory

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kt.fluxo.core.Bootstrapper
import kt.fluxo.core.FluxoIntent
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.SideJob
import kt.fluxo.core.factory.FluxoStoreFactory
import kt.fluxo.core.factory.StoreDecorator
import kt.fluxo.core.factory.StoreDecoratorBase
import kt.fluxo.core.internal.FluxoIntentHandler
import kt.fluxo.test.runUnitTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoreDecoratorTest {
    @Test
    fun decorator_interface_check() {
        val decorator = object : StoreDecorator<Any, Any, Any> {
            override fun init(decorator: StoreDecorator<Any, Any, Any>, conf: FluxoSettings<Any, Any, Any>) = TODO()
            override suspend fun onBootstrap(bootstrapper: Bootstrapper<Any, Any, Any>) = TODO()
            override suspend fun onStart(bootstrapper: Bootstrapper<Any, Any, Any>?) = TODO()
            override suspend fun onIntent(intent: Any) = TODO()
            override suspend fun onSideJob(key: String, wasRestarted: Boolean, sideJob: SideJob<Any, Any, Any>) = TODO()

            override var value: Any
                get() = TODO()
                set(@Suppress("UNUSED_PARAMETER") value) = TODO()

            override suspend fun sideJob(
                key: String,
                context: CoroutineContext,
                start: CoroutineStart,
                onError: ((error: Throwable) -> Unit)?,
                block: SideJob<Any, Any, Any>,
            ) = TODO()

            override suspend fun postSideEffect(sideEffect: Any) = TODO()
            override fun compareAndSet(expect: Any, update: Any) = TODO()
            override val sideEffectFlow: Flow<Any> get() = TODO()
            override val name: String get() = TODO()
            override val subscriptionCount: StateFlow<Int> get() = TODO()
            override fun start() = TODO()
            override suspend fun emit(value: Any) = TODO()
            override fun send(intent: Any) = TODO()
            override val replayCache: List<Any> get() = TODO()
            override val coroutineContext: CoroutineContext get() = TODO()
            override suspend fun collect(collector: FlowCollector<Any>) = TODO()
        }
        assertIs<StoreDecorator<*, *, *>>(decorator)
        assertIsNot<StoreDecoratorBase<*, *, *>>(decorator)

        // default interface implementations should be callable
        decorator.onStateChanged(state = Unit, fromState = Unit)
        decorator.onUndeliveredIntent(Unit, wasResent = false)
        decorator.onUndeliveredSideEffect(sideEffect = Unit, wasResent = false)
        assertFalse(decorator.onUnhandledError(AssertionError()))
        decorator.onClosed(AssertionError())

        val decorator2 = object : StoreDecoratorBase<Any, Any, Any>(store = decorator) {}
        assertIs<StoreDecorator<*, *, *>>(decorator2)
        assertIs<StoreDecoratorBase<*, *, *>>(decorator2)

        assertFailsWith<NotImplementedError> { decorator2.value }
        assertFailsWith<NotImplementedError> { decorator2.name }
        assertFailsWith<NotImplementedError> { decorator2.subscriptionCount }
        assertFailsWith<NotImplementedError> { decorator2.coroutineContext }
        assertFailsWith<NotImplementedError> { decorator2.replayCache }
        assertFailsWith<NotImplementedError> { decorator2.sideEffectFlow }
    }

    @Test
    @Suppress("LongMethod")
    fun interface_check() = runUnitTest {
        var bootstrapped = false
        var bootstrapIntercepted = false
        var stateChanged: Int? = null
        var intentSeen = false
        var sideEffectSeen = false
        var started = false

        val handler = FluxoIntentHandler<Int, Int>()
        val settings = FluxoSettings<FluxoIntent<Int, Int>, Int, Int>().apply {
            debugChecks = false
            scope = backgroundScope
            lazy = true
            onStart {
                bootstrapped = true
            }
        }
        val store = FluxoStoreFactory.createForDecoration(initialState = 0, handler = handler, settings = settings)
        assertIs<StoreDecorator<*, *, *>>(store)
        assertIsNot<StoreDecoratorBase<*, *, *>>(store)

        val decorator = object : StoreDecoratorBase<FluxoIntent<Int, Int>, Int, Int>(store) {
            override suspend fun onBootstrap(bootstrapper: Bootstrapper<FluxoIntent<Int, Int>, Int, Int>) {
                bootstrapIntercepted = true
                // no super call!
                // super.onBootstrap(bootstrapper)
            }

            override suspend fun onStart(bootstrapper: Bootstrapper<FluxoIntent<Int, Int>, Int, Int>?) {
                super.onStart(bootstrapper)
                started = true
            }

            override fun onStateChanged(state: Int, fromState: Int) {
                super.onStateChanged(state, fromState)
                stateChanged = state
            }

            override suspend fun onIntent(intent: FluxoIntent<Int, Int>) {
                super.onIntent(intent)
                intentSeen = true
            }

            override suspend fun postSideEffect(sideEffect: Int) {
                super.postSideEffect(sideEffect)
                sideEffectSeen = true
            }
        }
        decorator.init(decorator, settings)

        decorator.start().join()
        assertFalse(bootstrapped, "bootstrapped")
        assertTrue(bootstrapIntercepted, "bootstrapIntercepted")
        assertTrue(started, "started")

        assertTrue(decorator.name.startsWith("store#"), decorator.name)
        assertTrue(decorator.toString().startsWith("Fluxo[store#"), decorator.toString())
        assertTrue(decorator.toString().endsWith("]: 0"), decorator.toString())
        assertEquals(0, decorator.value)
        assertEquals(0, decorator.first())
        assertNull(stateChanged, "stateChanged")

        decorator.value = 1
        assertEquals(1, decorator.value)
        assertEquals(1, stateChanged, "stateChanged")
        assertTrue(decorator.toString().endsWith("]: 1"), decorator.toString())

        assertFalse(decorator.compareAndSet(0, 2), "compareAndSet(0, 2)")
        assertEquals(1, decorator.value)
        assertEquals(1, stateChanged, "stateChanged")

        assertTrue(decorator.compareAndSet(1, 2), "compareAndSet(1, 2)")
        assertEquals(2, decorator.value)
        assertEquals(2, stateChanged, "stateChanged")

        decorator.onStateChanged(state = 0, fromState = 0)
        assertEquals(0, stateChanged, "stateChanged")
        assertEquals(2, decorator.value)

        // should be callable
        decorator.onUndeliveredIntent({}, wasResent = false)
        decorator.onUndeliveredSideEffect(sideEffect = 0, wasResent = false)
        assertFalse(decorator.onUnhandledError(AssertionError()))
        decorator.onClosed(AssertionError())

        assertFalse(intentSeen, "intentSeen")
        decorator.send { value = 3 }.join()
        assertEquals(3, stateChanged, "stateChanged")
        assertEquals(3, decorator.value)
        assertTrue(intentSeen, "intentSeen")

        decorator.emit { value = 4 }
        yield()
        assertEquals(4, stateChanged, "stateChanged")
        assertEquals(4, decorator.value)
        assertEquals(4, decorator.first())

        assertFalse(sideEffectSeen, "sideEffectSeen")
        decorator.send { postSideEffect(-1) }
        assertEquals(-1, decorator.sideEffectFlow.first())
        assertTrue(sideEffectSeen, "sideEffectSeen")

        decorator.send {
            sideJob {
                value = 5
            }
        }
        assertEquals(5, decorator.first { it == 5 })
        assertEquals(5, decorator.value)
        assertEquals(5, stateChanged, "stateChanged")
    }

    @Test
    fun interface_check2() = runUnitTest {
        var bootstrapped = false
        var bootstrapIntercepted = false

        val handler = FluxoIntentHandler<Int, Int>()
        val settings = FluxoSettings<FluxoIntent<Int, Int>, Int, Int>().apply {
            scope = backgroundScope
            lazy = true
            onStart {
                bootstrapped = true
                noOp()
            }
        }
        val store = FluxoStoreFactory.createForDecoration(initialState = 0, handler = handler, settings = settings)
        val decorator = object : StoreDecoratorBase<FluxoIntent<Int, Int>, Int, Int>(store) {
            override suspend fun onBootstrap(bootstrapper: Bootstrapper<FluxoIntent<Int, Int>, Int, Int>) {
                bootstrapIntercepted = true
                super.onBootstrap(bootstrapper)
            }
        }
        // Deliberately wrong usage
        store.init(decorator, settings)

        decorator.start().join()
        assertTrue(bootstrapped, "bootstrapped")
        assertTrue(bootstrapIntercepted, "bootstrapIntercepted")

        decorator.close()
        assertFalse(decorator.isActive, "decorator.isActive")
        assertFalse(store.isActive, "store.isActive")
    }
}
