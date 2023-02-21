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
 *
 * _Example results:_
 * ```
 * Benchmark                                  Mode  Cnt   Score  Error   Units
 * decorator_interception                    thrpt   60   1.052 ±0.031  ops/ms
 * pipeline_interception                     thrpt   60   1.045 ±0.019  ops/ms
 * pipeline_interception_lambdas             thrpt   60   0.384 ±0.002  ops/ms
 *
 * decorator_interception                     avgt   60   6.508 ±0.132   ms/op
 * pipeline_interception                      avgt   60   6.650 ±0.105   ms/op
 * pipeline_interception_lambdas              avgt   60  18.480 ±0.200   ms/op
 *
 * decorator_interception                       ss   60   7.718 ±0.963   ms/op
 * pipeline_interception                        ss   60   7.918 ±0.816   ms/op
 * pipeline_interception_lambdas                ss   60  22.084 ±1.789   ms/op
 *
 *
 * pipeline_interception__creations          thrpt   60   1.205 ±0.009  ops/ms
 * decorator_interception__creations         thrpt   60   1.107 ±0.010  ops/ms
 * pipeline_interception_lambdas__creations  thrpt   60   0.394 ±0.002  ops/ms
 *
 * decorator_interception__creations          avgt   60   6.179 ±0.060   ms/op
 * pipeline_interception__creations           avgt   60   6.196 ±0.094   ms/op
 * pipeline_interception_lambdas__creations   avgt   60  17.638 ±0.062   ms/op
 *
 * pipeline_interception__creations             ss   60   6.614 ±0.321   ms/op
 * decorator_interception__creations            ss   60   7.762 ±0.710   ms/op
 * pipeline_interception_lambdas__creations     ss   60  19.717 ±1.258   ms/op
 * ```
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
     * * Basically 1.5-3x slower than simple interception chain.
     */
    @Benchmark
    fun pipeline_interception_lambdas(bh: Blackhole) =
        bh.consume(PipelineInterceptionProceedLambdaChain.test(creations = 10, interceptions = 3000))

    @Benchmark
    fun pipeline_interception_lambdas__creations(bh: Blackhole) =
        bh.consume(PipelineInterceptionProceedLambdaChain.test(creations = 3000, interceptions = 10))


    /**
     * **Decorator interception as in MVIKotlin (via factory) or in Orbit (via ContainerDecorator).**
     *
     * * Best performance
     * * Less convenient configuration
     */
    @Benchmark
    fun decorator_interception(bh: Blackhole) = bh.consume(DecoratorInterception.test(creations = 10, interceptions = 3000))

    @Benchmark
    fun decorator_interception__creations(bh: Blackhole) = bh.consume(DecoratorInterception.test(creations = 3000, interceptions = 10))


    /**
     * **Event stream as in Ballast.**
     *
     * * Allows to observe, but not intercept.
     * * Requires additional reactive machinery.
     */
}
