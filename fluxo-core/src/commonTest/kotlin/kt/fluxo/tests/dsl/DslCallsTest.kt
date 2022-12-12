package kt.fluxo.tests.dsl

import kt.fluxo.core.IntentHandler
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.store
import kt.fluxo.test.runUnitTest
import kotlin.test.Test

internal class DslCallsTest {
    @Test
    fun dsl_scoped_calls() = runUnitTest {
        container(0).closeAndWait()
        container(0) {}.closeAndWait()

        container<Int, Int>(0).closeAndWait()
        container<Int, Int>(0) {}.closeAndWait()

        store<Int, Int>(0, { 0 }).closeAndWait()
        store<Int, Int>(0, handler = {}).closeAndWait()
        store<Int, Int>(0, IntentHandler {}).closeAndWait()
        store<Int, Int, Int>(0, handler = {}).closeAndWait()
        store<Int, Int, Int>(0, {}).closeAndWait()
    }

    @Test
    fun dsl_simple_calls() = runUnitTest {
        dsl_simple_calls0()
    }

    private suspend fun dsl_simple_calls0() {
        container(0).closeAndWait()
        container(0) {}.closeAndWait()

        container<Int, Int>(0).closeAndWait()
        container<Int, Int>(0) {}.closeAndWait()

        store<Int, Int>(0, { 0 }).closeAndWait()
        store<Int, Int>(0, handler = {}).closeAndWait()
        store<Int, Int>(0, IntentHandler {}).closeAndWait()
        store<Int, Int, Int>(0, handler = {}).closeAndWait()
        store<Int, Int, Int>(0, {}).closeAndWait()
    }
}
