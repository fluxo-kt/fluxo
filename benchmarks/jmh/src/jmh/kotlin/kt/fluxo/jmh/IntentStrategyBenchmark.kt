@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package kt.fluxo.jmh

import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Unconfined
import kt.fluxo.core.intent.IntentStrategy.InBox.ChannelLifo
import kt.fluxo.core.intent.IntentStrategy.InBox.Direct
import kt.fluxo.core.intent.IntentStrategy.InBox.Fifo
import kt.fluxo.core.intent.IntentStrategy.InBox.Lifo
import kt.fluxo.core.intent.IntentStrategy.InBox.Parallel
import kt.fluxo.test.compare.fluxo.FluxoBenchmark
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

/**
 * Benchmark throughput for different intent strategies.
 *
 * [Lifo] strategies are tested only with [Unconfined] dispacher to not loosing any intents.
 *
 * _Example results:_
 * ```
 * Benchmark                                       Mode  Cnt   Score   Error   Units  Percent
 * direct__dispatcher_default                     thrpt   80   0.788 ± 0.025  ops/ms     0.0%
 * parallel__dispatcher_unconfined                thrpt   80   0.774 ± 0.003  ops/ms    -1.8%
 * direct__dispatcher_unconfined                  thrpt   80   0.712 ± 0.022  ops/ms    -9.6%
 * lifo__dispatcher_unconfined                    thrpt   80   0.609 ± 0.003  ops/ms   -22.7%
 * fifo__dispatcher_unconfined                    thrpt   80   0.248 ± 0.005  ops/ms   -68.5%
 * fifo__dispatcher_default                       thrpt   80   0.206 ± 0.001  ops/ms   -73.9%
 * channel_lifo_unordered__dispatcher_unconfined  thrpt   80   0.118 ± 0.001  ops/ms   -85.0%
 * channel_lifo_ordered__dispatcher_unconfined    thrpt   80   0.114 ± 0.001  ops/ms   -85.5%
 *
 * parallel__dispatcher_unconfined                 avgt   80   7.787 ± 0.089   ms/op     0.0%
 * direct__dispatcher_unconfined                   avgt   80   7.859 ± 0.172   ms/op     0.9%
 * direct__dispatcher_default                      avgt   80   7.963 ± 0.498   ms/op     2.3%
 * lifo__dispatcher_unconfined                     avgt   80   9.497 ± 0.056   ms/op    22.0%
 * fifo__dispatcher_unconfined                     avgt   80  25.160 ± 0.274   ms/op   223.1%
 * fifo__dispatcher_default                        avgt   80  28.957 ± 0.404   ms/op   271.9%
 * channel_lifo_unordered__dispatcher_unconfined   avgt   80  52.701 ± 1.100   ms/op   576.8%
 * channel_lifo_ordered__dispatcher_unconfined     avgt   80  54.015 ± 0.556   ms/op   593.7%
 *
 * direct__dispatcher_default                        ss   80  13.203 ± 1.752   ms/op     0.0%
 * direct__dispatcher_unconfined                     ss   80  13.570 ± 1.411   ms/op     2.8%
 * parallel__dispatcher_unconfined                   ss   80  14.018 ± 2.075   ms/op     6.2%
 * lifo__dispatcher_unconfined                       ss   80  17.662 ± 2.702   ms/op    33.8%
 * parallel__dispatcher_default                      ss   80  22.864 ± 4.057   ms/op    73.2%
 * fifo__dispatcher_unconfined                       ss   80  27.922 ± 1.777   ms/op   111.5%
 * fifo__dispatcher_default                          ss   80  40.881 ± 5.182   ms/op   209.6%
 * channel_lifo_ordered__dispatcher_unconfined       ss   80  53.439 ± 1.646   ms/op   304.7%
 * channel_lifo_unordered__dispatcher_unconfined     ss   80  54.447 ± 1.959   ms/op   312.4%
 * ```
 */
@State(Scope.Benchmark)
@Suppress("FunctionNaming", "FunctionName", "InjectDispatcher")
open class IntentStrategyBenchmark {
    @Benchmark
    fun fifo__dispatcher_default(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(strategy = Fifo, dispatcher = Default))

    @Benchmark
    fun fifo__dispatcher_unconfined(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(strategy = Fifo, dispatcher = Unconfined))


    @Benchmark
    fun lifo__dispatcher_unconfined(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(strategy = Lifo, dispatcher = Unconfined))

    @Benchmark
    fun channel_lifo_ordered__dispatcher_unconfined(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(strategy = ChannelLifo(ordered = true), dispatcher = Unconfined))

    @Benchmark
    fun channel_lifo_unordered__dispatcher_unconfined(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(strategy = ChannelLifo(ordered = false), dispatcher = Unconfined))


    @Benchmark
    fun parallel__dispatcher_default(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(strategy = Parallel, dispatcher = Default))

    @Benchmark
    fun parallel__dispatcher_unconfined(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(strategy = Parallel, dispatcher = Unconfined))


    @Benchmark
    fun direct__dispatcher_default(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(strategy = Direct, dispatcher = Default))

    @Benchmark
    fun direct__dispatcher_unconfined(bh: Blackhole) =
        bh.consume(FluxoBenchmark.mviReducerStaticIncrement(strategy = Direct, dispatcher = Unconfined))
}
