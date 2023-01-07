package kt.fluxo.jmh.arch

import kt.fluxo.test.interception.DecoratorInterception
import kt.fluxo.test.interception.PipelineInterceptionChain
import kt.fluxo.test.interception.PipelineInterceptionProceedLambdaChain
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

/**
 * Research on different interception types performance
 */
@State(Scope.Benchmark)
@Suppress("FunctionNaming", "FunctionName")
open class InterceptionTypesBenchmark {
    /**
     * **Complete pipeline interception as used in OkHttp, etc.**
     *
     * * Also has some similarities with Netty ChannelPipeline/ChannelHandler.
     */
    @Benchmark
    fun pipeline_interception(bh: Blackhole) = bh.consume(PipelineInterceptionChain.test(creations = 10, interceptions = 3000))

    @Benchmark
    fun pipeline_interception__creations(bh: Blackhole) = bh.consume(PipelineInterceptionChain.test(creations = 3000, interceptions = 10))


    /**
     * **Complete pipeline interception, proceed with lambdas**
     *
     * * Doesn't require multiple implementaions for each action as simple interception chain
     * * Usage code is harder to read and maintain
     * * Basically 1.5-2x slower than simple interception chain.
     */
    @Benchmark
    fun pipeline_interception_lambdas(bh: Blackhole) =
        bh.consume(PipelineInterceptionProceedLambdaChain.test(creations = 10, interceptions = 3000))

    @Benchmark
    fun pipeline_interception_lambdas__creations(bh: Blackhole) =
        bh.consume(PipelineInterceptionProceedLambdaChain.test(creations = 3000, interceptions = 10))


    /**
     * **Decorator interception as in MVIKotlin (via factory) or in Orbit (via ContainerDecorator).**
     */
    @Benchmark
    fun decorator_interception(bh: Blackhole) =
        bh.consume(DecoratorInterception.test(creations = 10, interceptions = 3000))

    @Benchmark
    fun decorator_interception__creations(bh: Blackhole) =
        bh.consume(DecoratorInterception.test(creations = 3000, interceptions = 10))


    /**
     * **Event stream as in Ballast.**
     *
     * * Allows to observe, but not intercept.
     * * Requires additional reactive machinery.
     */
}
