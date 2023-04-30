package kt.fluxo.jmh

import kotlinx.coroutines.Dispatchers
import kt.fluxo.core.intent.IntentStrategy.InBox.Parallel
import kt.fluxo.test.compare.fluxo.FluxoBenchmark
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

/**
 * Benchmark throughput for different coroutine dispatchers.
 *
 * _Example results:_
 * ```
 * Benchmark                             Mode  Cnt   Score   Error   Units  Percent
 * fluxo__dispatchers_unconfined        thrpt  125   0.237 ± 0.003  ops/ms     0.0%
 * fluxo__dispatchers_default_limited2  thrpt  125   0.209 ± 0.005  ops/ms   -11.8%
 * fluxo__dispatchers_io                thrpt  125   0.209 ± 0.005  ops/ms   -11.8%
 * fluxo__single_thread_context         thrpt  125   0.209 ± 0.004  ops/ms   -11.8%
 * fluxo__dispatchers_default           thrpt  125   0.206 ± 0.005  ops/ms   -13.1%
 *
 * fluxo__dispatchers_unconfined         avgt  125  21.818 ± 0.251   ms/op     0.0%
 * fluxo__dispatchers_default            avgt  125  24.027 ± 0.410   ms/op    10.1%
 * fluxo__dispatchers_io                 avgt  125  24.194 ± 0.404   ms/op    10.9%
 * fluxo__dispatchers_default_limited2   avgt  125  24.973 ± 0.647   ms/op    14.5%
 * fluxo__single_thread_context          avgt  125  25.012 ± 0.294   ms/op    14.6%
 *
 * fluxo__dispatchers_unconfined           ss  125  24.244 ± 0.533   ms/op     0.0%
 * fluxo__single_thread_context            ss  125  26.428 ± 2.177   ms/op     9.0%
 * fluxo__dispatchers_default              ss  125  28.134 ± 2.430   ms/op    16.0%
 * fluxo__dispatchers_default_limited2     ss  125  28.707 ± 3.159   ms/op    18.4%
 * fluxo__dispatchers_io                   ss  125  30.483 ± 3.040   ms/op    25.7%
 * ```
 */
@State(Scope.Benchmark)
@Suppress("FunctionNaming", "FunctionName", "InjectDispatcher")
open class CoroutineDispatcherBenchmark {
    @Benchmark
    fun fluxo__single_thread_context(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(dispatcher = null, strategy = Parallel))

    @Benchmark
    fun fluxo__dispatchers_default(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(dispatcher = Dispatchers.Default, strategy = Parallel))

    @Benchmark
    fun fluxo__dispatchers_default_limited2(bh: Blackhole) = bh.consume(
        FluxoBenchmark.mviReducerStaticIncrement(dispatcher = Dispatchers.Default.limitedParallelism(2), strategy = Parallel),
    )

    @Benchmark
    fun fluxo__dispatchers_io(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(dispatcher = Dispatchers.IO, strategy = Parallel))

    @Benchmark
    fun fluxo__dispatchers_unconfined(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(dispatcher = Dispatchers.Unconfined, strategy = Parallel))
}
