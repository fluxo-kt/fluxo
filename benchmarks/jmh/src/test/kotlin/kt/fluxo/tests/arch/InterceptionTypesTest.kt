package kt.fluxo.tests.arch

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kt.fluxo.test.interception.DecoratorInterception
import kt.fluxo.test.interception.PipelineInterceptionChain
import kt.fluxo.test.interception.PipelineInterceptionProceedLambdaChain
import org.junit.Test

@Suppress("FunctionNaming")
class InterceptionTypesTest {

    @Test
    fun pipeline_interception_simple() = test { PipelineInterceptionChain.test() }

    @Test
    fun pipeline_interception_proceed_lambda() = test { PipelineInterceptionProceedLambdaChain.test() }

    @Test
    fun decorator_interception() = test { DecoratorInterception.test() }


    private fun test(testBody: suspend TestScope.() -> Unit) = runTest(dispatchTimeoutMs = 1_000, testBody = testBody)
}
