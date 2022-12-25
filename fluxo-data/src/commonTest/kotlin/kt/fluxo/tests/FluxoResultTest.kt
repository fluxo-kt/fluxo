package kt.fluxo.tests

import kt.fluxo.data.FluxoResult
import kt.fluxo.data.FluxoResult.Companion.cached
import kt.fluxo.data.FluxoResult.Companion.empty
import kt.fluxo.data.FluxoResult.Companion.failure
import kt.fluxo.data.FluxoResult.Companion.loading
import kt.fluxo.data.FluxoResult.Companion.notLoaded
import kt.fluxo.data.FluxoResult.Companion.success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FluxoResultTest {
    @Test
    fun not_loaded_state() {
        for (it in arrayOf(notLoaded(), notLoaded(value = null))) {
            assertTrue(it.isNotLoaded)
            assertTrue(it.isEmpty)
            assertFalse(it.isCached)
            assertFalse(it.isLoading)
            assertFalse(it.isSuccess)
            assertFalse(it.isFailure)
            assertFalse(it.isFailed)
            assertNull(it.value)
            assertNull(it.error)
            assertEquals("NotLoaded", it.toString())
        }
        notLoaded(value = "Value").let {
            assertTrue(it.isNotLoaded)
            assertFalse(it.isEmpty)
            assertFalse(it.isCached)
            assertFalse(it.isLoading)
            assertFalse(it.isSuccess)
            assertFalse(it.isFailure)
            assertFalse(it.isFailed)
            assertEquals("Value", it.value)
            assertNull(it.error)
            assertEquals("NotLoaded(Value)", it.toString())
        }
        for (it in arrayOf(notLoaded(emptyList<Nothing>()), notLoaded(listOf()))) {
            assertTrue(it.isNotLoaded)
            assertTrue(it.isEmpty)
            assertFalse(it.isCached)
            assertFalse(it.isLoading)
            assertFalse(it.isSuccess)
            assertFalse(it.isFailure)
            assertFalse(it.isFailed)
            assertTrue(it.value.isEmpty())
            assertNull(it.error)
            assertEquals("NotLoaded([])", it.toString())
        }
        notLoaded(value = "").let {
            assertTrue(it.isNotLoaded)
            assertTrue(it.isEmpty)
            assertFalse(it.isCached)
            assertFalse(it.isLoading)
            assertFalse(it.isSuccess)
            assertFalse(it.isFailure)
            assertFalse(it.isFailed)
            assertEquals("", it.value)
            assertNull(it.error)
            assertEquals("NotLoaded()", it.toString())
        }
    }

    @Test
    fun cached_state() {
        cached(value = null).let {
            assertFalse(it.isNotLoaded)
            assertTrue(it.isEmpty)
            assertTrue(it.isCached)
            assertFalse(it.isLoading)
            assertTrue(it.isSuccess)
            assertFalse(it.isFailure)
            assertFalse(it.isFailed)
            assertNull(it.value)
            assertNull(it.error)
            assertEquals("Cached", it.toString())
        }
        cached(value = "Value").let {
            assertFalse(it.isNotLoaded)
            assertFalse(it.isEmpty)
            assertTrue(it.isCached)
            assertFalse(it.isLoading)
            assertTrue(it.isSuccess)
            assertFalse(it.isFailure)
            assertFalse(it.isFailed)
            assertEquals("Value", it.value)
            assertNull(it.error)
            assertEquals("Cached(Value)", it.toString())
        }
    }

    @Test
    fun loading_state() {
        for (it in arrayOf(loading(), loading(value = null))) {
            assertFalse(it.isNotLoaded)
            assertTrue(it.isEmpty)
            assertFalse(it.isCached)
            assertTrue(it.isLoading)
            assertFalse(it.isSuccess)
            assertFalse(it.isFailure)
            assertFalse(it.isFailed)
            assertNull(it.value)
            assertNull(it.error)
            assertEquals("Loading", it.toString())
        }
        loading(value = "Value").let {
            assertFalse(it.isNotLoaded)
            assertFalse(it.isEmpty)
            assertFalse(it.isCached)
            assertTrue(it.isLoading)
            assertTrue(it.isSuccess)
            assertFalse(it.isFailure)
            assertFalse(it.isFailed)
            assertEquals("Value", it.value)
            assertNull(it.error)
            assertEquals("Loading(Value)", it.toString())
        }
    }

    @Test
    fun empty_state() {
        for (it in arrayOf(empty(), empty(value = null))) {
            assertFalse(it.isNotLoaded)
            assertTrue(it.isEmpty)
            assertFalse(it.isCached)
            assertFalse(it.isLoading)
            assertTrue(it.isSuccess)
            assertFalse(it.isFailure)
            assertFalse(it.isFailed)
            assertNull(it.value)
            assertNull(it.error)
            assertEquals("Empty", it.toString())
        }
        empty(value = "Value").let {
            assertFalse(it.isNotLoaded)
            assertTrue(it.isEmpty)
            assertFalse(it.isCached)
            assertFalse(it.isLoading)
            assertTrue(it.isSuccess)
            assertFalse(it.isFailure)
            assertFalse(it.isFailed)
            assertEquals("Value", it.value)
            assertNull(it.error)
            assertEquals("Empty(Value)", it.toString())
        }
        assertEquals(empty(), empty(value = null))
        assertEquals(empty(), success(value = null))
    }

    @Test
    fun success_state() {
        success(value = "Value").let {
            assertFalse(it.isNotLoaded)
            assertFalse(it.isEmpty)
            assertFalse(it.isCached)
            assertFalse(it.isLoading)
            assertTrue(it.isSuccess)
            assertFalse(it.isFailure)
            assertFalse(it.isFailed)
            assertEquals("Value", it.value)
            assertNull(it.error)
            assertEquals("Success(Value)", it.toString())
        }
        assertEquals(empty(), success(value = null))
        success(value = null).let {
            assertFalse(it.isNotLoaded)
            assertTrue(it.isEmpty)
            assertFalse(it.isCached)
            assertFalse(it.isLoading)
            assertTrue(it.isSuccess)
            assertFalse(it.isFailure)
            assertFalse(it.isFailed)
            assertNull(it.value)
            assertNull(it.error)
            assertEquals("Empty", it.toString())
        }
    }

    @Test
    fun failed_state() {
        failure(null).let {
            assertFalse(it.isNotLoaded)
            assertTrue(it.isEmpty)
            assertFalse(it.isCached)
            assertFalse(it.isLoading)
            assertFalse(it.isSuccess)
            assertTrue(it.isFailure)
            assertTrue(it.isFailed)
            assertNull(it.value)
            assertNull(it.error)
            assertEquals("Failure", it.toString())
        }
        failure(error = null, value = "Value").let {
            assertFalse(it.isNotLoaded)
            assertFalse(it.isEmpty)
            assertFalse(it.isCached)
            assertFalse(it.isLoading)
            assertFalse(it.isSuccess)
            assertTrue(it.isFailure)
            assertTrue(it.isFailed)
            assertEquals("Value", it.value)
            assertNull(it.error)
            assertEquals("Failure(Value)", it.toString())
        }
        val error = RuntimeException("Message")
        failure(error = error, value = "Value").let {
            assertFalse(it.isNotLoaded)
            assertFalse(it.isEmpty)
            assertFalse(it.isCached)
            assertFalse(it.isLoading)
            assertFalse(it.isSuccess)
            assertTrue(it.isFailure)
            assertTrue(it.isFailed)
            assertEquals("Value", it.value)
            assertEquals("Message", assertNotNull(it.error).message)
            val string = it.toString()
            assertTrue(string.startsWith("Failure(Value"))
            assertTrue(string.endsWith("Message)"))
        }
        failure(error = error).let {
            assertFalse(it.isNotLoaded)
            assertTrue(it.isEmpty)
            assertFalse(it.isCached)
            assertFalse(it.isLoading)
            assertFalse(it.isSuccess)
            assertTrue(it.isFailure)
            assertTrue(it.isFailed)
            assertNull(it.value)
            assertEquals("Message", assertNotNull(it.error).message)
            val string = it.toString()
            assertTrue(string.startsWith("Failure("))
            assertTrue(string.endsWith("Message)"))
        }

        assertTrue(FluxoResult(null, error, 0).isFailure)
        assertTrue(FluxoResult(null, error, 0).isFailed)
    }


    @Test
    fun auto_detection_of_empty_collections() {
        for (it in arrayOf<Collection<Any?>>(
            emptyList(),
            listOf(),
            mutableListOf(),
            arrayListOf(),
            emptySet(),
            setOf(),
            mutableSetOf(),
            emptyMap<Any, Any>().keys,
            mapOf<Any, Any>().values,
            mutableMapOf<Any, Any>().entries,
        ).flatMap(::packAsData)) {
            assertTrue(it.isEmpty)
            assertNull(it.error)
            assertTrue(it.value.isEmpty())
        }

        for (it in arrayOf<Collection<Any?>>(
            listOf(""),
            mutableListOf(""),
            arrayListOf(""),
            setOf(""),
            mutableSetOf(""),
            mapOf("" to "").values,
            mutableMapOf("" to "").entries,
        ).flatMap(::packAsNotEmptyData)) {
            assertFalse(it.isEmpty)
            assertNull(it.error)
            assertFalse(it.value.isEmpty())
        }
    }

    @Test
    fun auto_detection_of_empty_char_sequences() {
        for (it in arrayOf(
            "",
            String(),
            StringBuilder(),
        ).flatMap(::packAsData)) {
            assertTrue(it.isEmpty)
            assertNull(it.error)
            assertTrue(it.value.isEmpty())
        }

        for (it in arrayOf(
            "a",
            StringBuilder("a"),
        ).flatMap(::packAsNotEmptyData)) {
            assertFalse(it.isEmpty)
            assertNull(it.error)
            assertFalse(it.value.isEmpty())
        }
    }

    @Test
    fun auto_detection_of_empty_null() {
        for (it in arrayOfNulls<Any>(1).flatMap(::packAsData)) {
            assertTrue(it.isEmpty)
            assertNull(it.error)
            assertNull(it.value)
        }
    }

    private fun <T> packAsData(it: T) = listOf(
        notLoaded(it), empty(it), cached(it), loading(it), success(it), failure(null, it),
    )

    private fun <T> packAsNotEmptyData(it: T) = listOf(
        notLoaded(it), cached(it), loading(it), success(it), failure(null, it),
    )


    @Test
    fun data_class_api() {
        assertNotNull(FluxoResult.Companion)

        val f = failure(null, "")
        f.let { (data) -> assertEquals("", data) }
        f.let { (_, exc) -> assertNull(exc) }
    }
}
