package kt.fluxo.jmh.arch

import kt.fluxo.test.interception.PipelineInterceptionChain
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

/**
 * Research on different interception types performance
 */
@State(Scope.Benchmark)
@Suppress("FunctionNaming", "FunctionName")
class InterceptionTypesBenchmark {
    /**
     * **Complete pipeline interception as used in OkHttp, etc.**
     */
    fun pipeline_interception_simple(bh: Blackhole) = bh.consume(PipelineInterceptionChain.test())

    /**
     * **Complete pipeline interception, proceed with lambdas**
     *
     * Complicated code, not easy to maintain and read.
     */
    fun pipeline_interception_lambda() {
        TODO()
    }

    /**
     * **Store decorator as in MVIKotlin (via factory) or in Orbit (via internal decorator).**
     */
    fun decorator() {
        TODO()
    }

    /**
     * **Event stream as in Ballast.**
     *
     * Allows to observe, but not directly intercept.
     * Requires additional reactive machinery.
     */
    fun event_stream() {
        TODO()
    }
}
