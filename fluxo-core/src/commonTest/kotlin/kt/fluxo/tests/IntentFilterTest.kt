package kt.fluxo.tests

import kotlinx.coroutines.flow.first
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.store
import kt.fluxo.test.runUnitTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals


class IntentFilterTest {
    @Test
    fun filtration_should_work() = runUnitTest {
        val results = ArrayList<Int>()
        val filter: (Int) -> Boolean = { it % 2 == 0 }
        val store = store(
            initialState = 0,
            handler = { intent: Int ->
                results.add(intent)
                updateState { intent }
            },
            setup = {
                intentFilter = { filter(it) }
            },
        )
        repeat(1000, store::send)
        store.first { it == 998 }
        assertEquals(500, results.size)
        assertContentEquals(Array(1000) { it }.filter(filter), results)
        store.closeAndWait()
    }
}
