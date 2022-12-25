package kt.fluxo.tests

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kt.fluxo.data.FluxoResult.Companion.empty
import kt.fluxo.data.FluxoResult.Companion.failure
import kt.fluxo.data.FluxoResult.Companion.loading
import kt.fluxo.data.FluxoResult.Companion.notLoaded
import kt.fluxo.data.FluxoResult.Companion.success
import kt.fluxo.data.filterResult
import kt.fluxo.data.getOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FluxoResultDslTest {

    @Test
    fun extensions() {
        assertNull(empty().getOrThrow())

        assertFailsWith<IllegalStateException> {
            failure(null).getOrThrow()
        }

        assertFailsWith<ArithmeticException> {
            failure(ArithmeticException()).getOrThrow()
        }
    }

    @Test
    fun flow_filter_result() = runTest {
        sequence { yieldAll(listOf(notLoaded(), loading(), success(1))) }.asFlow().filterResult().first().run {
            assertTrue(isSuccess)
            assertEquals(1, value)
        }

        sequence { yield(notLoaded(1)) }.asFlow().filterResult().first().run {
            assertTrue(isNotLoaded)
            assertEquals(1, value)
        }

        sequence { yield(loading(1)) }.asFlow().filterResult().first().run {
            assertTrue(isLoading)
            assertEquals(1, value)
        }

        sequence { yieldAll(listOf(notLoaded(), failure(error = null, value = 1))) }.asFlow().filterResult().first().run {
            assertTrue(isFailure)
            assertEquals(1, value)
        }
    }
}
