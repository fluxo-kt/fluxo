package kt.fluxo.tests.arch

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kt.fluxo.test.interception.PipelineInterceptionChain
import kt.fluxo.test.interception.PipelineInterceptionProceedLambdaChain
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("FunctionNaming")
class InterceptionTypesTest {

    @Test
    fun pipeline_interception_simple() = testResult { PipelineInterceptionChain.test() }

    @Test
    fun pipeline_interception_proceed_lambda() = test { PipelineInterceptionProceedLambdaChain.test() }


    private fun test(testBody: suspend TestScope.() -> Unit) = runTest(dispatchTimeoutMs = 1_000, testBody = testBody)

    private fun testResult(testBody: suspend TestScope.() -> Int) = runTest(dispatchTimeoutMs = 1_000) {
        assertEquals(EXPECTED_RESULT, testBody())
    }

    private companion object {
        private const val EXPECTED_RESULT = 15
    }
}
