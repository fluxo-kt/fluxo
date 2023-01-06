package kt.fluxo.tests.arch

import kotlinx.coroutines.test.runTest
import kt.fluxo.test.interception.PipelineInterceptionChain
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("FunctionNaming")
class InterceptionTypesTest {

    @Test
    fun pipeline_interception_simple() = test(PipelineInterceptionChain.test())

    private fun test(result: Int) = runTest(dispatchTimeoutMs = 1_000) {
        assertEquals(EXPECTED_RESULT, result)
    }

    private companion object {
        private const val EXPECTED_RESULT = 15
    }
}
