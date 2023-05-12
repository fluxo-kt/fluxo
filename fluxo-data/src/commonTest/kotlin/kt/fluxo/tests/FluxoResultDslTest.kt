@file:Suppress("ConstantConditionIf", "ThrowsCount")

package kt.fluxo.tests

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kt.fluxo.data.FluxoResult
import kt.fluxo.data.FluxoResult.Companion.empty
import kt.fluxo.data.FluxoResult.Companion.failure
import kt.fluxo.data.FluxoResult.Companion.loading
import kt.fluxo.data.FluxoResult.Companion.notLoaded
import kt.fluxo.data.FluxoResult.Companion.success
import kt.fluxo.data.cached
import kt.fluxo.data.failure
import kt.fluxo.data.filterResult
import kt.fluxo.data.fold
import kt.fluxo.data.getOrDefault
import kt.fluxo.data.getOrElse
import kt.fluxo.data.getOrThrow
import kt.fluxo.data.isValid
import kt.fluxo.data.loading
import kt.fluxo.data.map
import kt.fluxo.data.mapCatching
import kt.fluxo.data.onFailure
import kt.fluxo.data.onSuccess
import kt.fluxo.data.recover
import kt.fluxo.data.recoverCatching
import kt.fluxo.data.resultOf
import kt.fluxo.data.success
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class FluxoResultDslTest {

    @Test
    fun producers() {
        assertEquals(empty(), resultOf { null })
        assertEquals(empty(), null.resultOf { this })

        assertFailsWith<CancellationException> {
            resultOf { throw CancellationException() }
        }
        assertFailsWith<CancellationException> {
            null.resultOf { throw CancellationException() }
        }

        assertFailsWith<IllegalStateException> {
            resultOf { checkNotNull<Unit>(null) }.also {
                assertTrue(it.isFailure)
            }.getOrThrow()
        }
        assertFailsWith<IllegalStateException> {
            null.resultOf { checkNotNull<Unit>(this) }.also {
                assertTrue(it.isFailure)
            }.getOrThrow()
        }

        val t = Throwable()
        val r = failure(t).resultOf { check(!isFailure) }
        assertNotEquals(t, r.error)
        assertSame(t, r.error?.suppressedExceptions?.firstOrNull())

        assertEquals(listOf(), null.resultOf { fail() }.error?.suppressedExceptions)
        assertEquals(listOf(), 1.resultOf { fail() }.error?.suppressedExceptions)
        assertEquals(listOf(), empty().resultOf { check(!isEmpty) }.error?.suppressedExceptions)
        assertEquals(listOf(), success(1).resultOf { check(!isSuccess) }.error?.suppressedExceptions)
        assertEquals(listOf(), failure(null).resultOf { check(!isFailure) }.error?.suppressedExceptions)
    }

    @Test
    fun extensions() {
        assertNull(empty().getOrThrow())

        assertFailsWith<IllegalStateException> {
            failure(null).getOrThrow()
        }

        assertFailsWith<ArithmeticException> {
            failure(ArithmeticException()).getOrThrow()
        }

        assertEquals(1, failure(null).getOrElse { 1 })
        assertEquals(1, success(1).getOrElse { 2 })
        assertNull(success(null).getOrElse { 2 })
        assertNull(empty().getOrElse { 2 })

        assertEquals(1, failure(null).getOrDefault { 1 })
        assertEquals(1, success(1).getOrDefault { 2 })
        assertEquals(2, success(null).getOrDefault { 2 })
        assertEquals(2, empty().getOrDefault { 2 })
        assertEquals(2, FluxoResult(null, null, 0).getOrDefault { 2 })

        assertEquals(1, success(null).fold(onSuccess = { 1 }, onFailure = { 2 }))
        assertEquals(2, failure(null).fold(onSuccess = { 1 }, onFailure = { 2 }))

        assertTrue(failure(null).isValid { true })
        assertFalse(empty().isValid { false })

        assertTrue(failure(null).cached().isCached)
        assertTrue(empty().loading().isLoading)
        assertTrue(failure(null).success().isSuccess)
        assertTrue(success(null).failure().isFailure)
    }

    @Test
    fun extensions_part2() {
        assertEquals(2, success(1).map { 2 }.value)
        assertEquals(1, failure(null, 1).map { 2 }.value)

        assertEquals(2, success(1).mapCatching { 2 }.value)
        assertEquals(1, failure(null, 1).mapCatching { 2 }.value)
        assertTrue(success(1).mapCatching { fail() }.isFailure)
        assertFailsWith<CancellationException> {
            success(1).mapCatching { throw CancellationException() }
        }

        assertEquals(1, success(1).recover { 2 }.value)
        assertEquals(2, failure(null, 1).recover { 2 }.value)

        assertEquals(1, success(1).recoverCatching { 2 }.value)
        assertEquals(1, success(1).recoverCatching { throw CancellationException() }.value)
        assertEquals(2, failure(null).recoverCatching { 2 }.value)
        assertEquals("boom", failure(null, 1).recoverCatching { fail("boom") }.error?.message)
        assertFailsWith<CancellationException> {
            failure(null).recoverCatching { throw CancellationException() }
        }

        assertFailsWith<CancellationException> {
            success(1).onFailure { fail() }.onSuccess { throw CancellationException() }
        }
        assertFailsWith<CancellationException> {
            failure(null).onSuccess { fail() }.onFailure { throw CancellationException() }
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
