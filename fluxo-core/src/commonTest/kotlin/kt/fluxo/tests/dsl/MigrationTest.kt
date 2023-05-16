package kt.fluxo.tests.dsl

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kt.fluxo.core.Container
import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.container
import kt.fluxo.core.dsl.ContainerHostS
import kt.fluxo.core.dsl.accept
import kt.fluxo.core.dsl.intentDispatcher
import kt.fluxo.core.dsl.orbit
import kt.fluxo.core.dsl.store
import kt.fluxo.core.store
import kt.fluxo.test.runUnitTest
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@Suppress("DEPRECATION")
class MigrationTest {
    @Test
    fun container_host__store() {
        val host = object : ContainerHostS<Int> {
            override val container: Container<Int, Nothing> get() = TODO()
        }
        assertFailsWith<NotImplementedError> {
            host.store
        }
    }

    @Test
    fun container__orbit() = runUnitTest {
        val store = backgroundScope.container(0)
        store.test {
            assertEquals(0, awaitItem())
            store.orbit { value = 1 }
            assertEquals(1, awaitItem())
        }
    }

    @Test
    fun settings__intent_dispatcher() {
        val settings = FluxoSettings.invoke<Int, Int, Int>()
        assertSame(Dispatchers.Default, settings.intentDispatcher)

        settings.intentDispatcher = Dispatchers.Main
        assertSame(Dispatchers.Main, settings.intentDispatcher)
        assertSame(Dispatchers.Main, settings.coroutineContext)

        settings.coroutineContext = EmptyCoroutineContext
        assertSame(Dispatchers.Default, settings.intentDispatcher)

        settings.scope = CoroutineScope(EmptyCoroutineContext)
        assertSame(Dispatchers.Default, settings.intentDispatcher)

        settings.scope = CoroutineScope(Dispatchers.Main)
        assertSame(Dispatchers.Main, settings.intentDispatcher)

        settings.scope = null
        assertSame(Dispatchers.Default, settings.intentDispatcher)

        settings.scope = CoroutineScope(Dispatchers.Main)
        assertSame(Dispatchers.Main, settings.intentDispatcher)

        settings.intentDispatcher = Dispatchers.Default
        assertSame(Dispatchers.Default, settings.intentDispatcher)
    }

    @Test
    fun store__accept() = runUnitTest {
        val store = backgroundScope.store(0, reducer = { intent: Int -> intent })
        store.test {
            assertEquals(0, awaitItem())
            store.accept(1)
            assertEquals(1, awaitItem())
        }
    }
}
