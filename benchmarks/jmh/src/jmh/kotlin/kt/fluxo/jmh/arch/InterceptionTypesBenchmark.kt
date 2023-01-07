package kt.fluxo.jmh.arch

import kt.fluxo.test.interception.PipelineInterceptionChain
import kt.fluxo.test.interception.PipelineInterceptionProceedLambdaChain
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
     * * Code is harder to maintain and read.
     */
    fun pipeline_interception_lambdas(bh: Blackhole) = bh.consume(PipelineInterceptionProceedLambdaChain.test())

    /**
     * **Netty-like pipeline**
     */
    private fun netty_pipeline() {
        // https://netty.io/4.0/api/io/netty/channel/ChannelPipeline.html
        // https://netty.io/4.0/api/io/netty/channel/ChannelHandler.html
        // https://netty.io/4.0/api/io/netty/channel/ChannelHandlerContext.html
        TODO()
    }

    /**
     * **Store decorator as in MVIKotlin (via factory) or in Orbit (via internal decorator).**
     */
    private fun decorator() {
        TODO()
    }

    /**
     * **Event stream as in Ballast.**
     *
     * Allows to observe, but not directly intercept.
     * Requires additional reactive machinery.
     */
    private fun event_stream() {
        TODO()
    }
}
