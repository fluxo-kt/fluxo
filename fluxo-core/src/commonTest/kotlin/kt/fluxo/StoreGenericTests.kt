package kt.fluxo

import kotlinx.coroutines.test.runTest
import kt.fluxo.core.Bootstrapper
import kt.fluxo.core.Reducer
import kt.fluxo.core.Store
import kt.fluxo.core.store
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class StoreGenericTests {

    private val storeFactory: (
        initialState: String,
        bootstrapper: Bootstrapper<String, String, Nothing>?,
        reducer: Reducer<String, String>,
    ) -> Store<String, String, String> = { initialState, bootstrapper, reducer ->
        store(
            initialState = initialState,
            reducer = reducer,
        ) {
            this.bootstrapper = bootstrapper
        }
    }

    @Test
    fun state_val_returns_initial_state_WHEN_created() {
        val initial = "initial"
        val store = store(initialState = initial)
        assertEquals(initial, store.state)
    }

    @Test
    fun initializes_bootstrapper_WHEN_created() = runTest {
        var isInitialized = false
        val store = store(bootstrapper = {
            isInitialized = true
        })
        store.start()?.join()
        assertTrue(isInitialized)
    }

    private fun store(
        initialState: String = "initial_state",
        bootstrapper: Bootstrapper<String, String, Nothing>? = null,
        reducer: Reducer<String, String> = { this }
    ): Store<String, String, String> {
        return storeFactory(initialState, bootstrapper, reducer)
    }
}
