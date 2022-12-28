@file:Suppress("FunctionNaming", "FunctionName", "TrailingCommaOnCallSite")

package kt.fluxo.jmh

import com.arkivanov.mvikotlin.core.store.create
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.badoo.mvicore.feature.ReducerFeature
import com.copperleaf.ballast.BallastViewModelConfiguration
import com.copperleaf.ballast.EventHandler
import com.copperleaf.ballast.EventHandlerScope
import com.copperleaf.ballast.InputHandler
import com.copperleaf.ballast.InputHandlerScope
import com.copperleaf.ballast.build
import com.copperleaf.ballast.core.BasicViewModel
import com.copperleaf.ballast.core.FifoInputStrategy
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kt.fluxo.core.Store
import kt.fluxo.core.annotation.ExperimentalFluxoApi
import kt.fluxo.core.closeAndWait
import kt.fluxo.core.container
import kt.fluxo.core.dsl.StoreScope
import kt.fluxo.core.internal.Closeable
import kt.fluxo.core.store
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole
import org.orbitmvi.orbit.syntax.simple.SimpleSyntax
import org.orbitmvi.orbit.syntax.simple.reduce
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.orbitmvi.orbit.ContainerHost as OrbitContainerHost
import org.orbitmvi.orbit.container as orbitContainer
import org.orbitmvi.orbit.syntax.simple.intent as orbitIntent

/**
 * Benchmark for simple incrementing intent throughput.
 *
 * _Example results:_
 * ```
 * Benchmark                                          Mode  Cnt    Score    Error   Units
 * IncrementIntentsBenchmark.mvicore__mvi_reducer    thrpt    6    2.399 ±  0.652  ops/ms
 * IncrementIntentsBenchmark.mvikotlin__mvi_reducer  thrpt    6    0.428 ±  0.009  ops/ms
 * IncrementIntentsBenchmark.ballast__mvi_handler    thrpt    6    0.417 ±  0.038  ops/ms
 * IncrementIntentsBenchmark.orbit__mvvm_intent      thrpt    6    0.203 ±  0.006  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvi_handler      thrpt    6    0.179 ±  0.018  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvi_reducer      thrpt    6    0.175 ±  0.011  ops/ms
 * IncrementIntentsBenchmark.fluxo__mvvm_intent      thrpt    6    0.018 ±  0.003  ops/ms
 *
 * IncrementIntentsBenchmark.mvicore__mvi_reducer     avgt    6    0.377 ±  0.032   ms/op
 * IncrementIntentsBenchmark.mvikotlin__mvi_reducer   avgt    6    2.409 ±  0.127   ms/op
 * IncrementIntentsBenchmark.ballast__mvi_handler     avgt    6    2.517 ±  0.247   ms/op
 * IncrementIntentsBenchmark.orbit__mvvm_intent       avgt    6    5.436 ±  0.501   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_reducer       avgt    6    5.561 ±  0.788   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_handler       avgt    6    5.768 ±  1.268   ms/op
 * IncrementIntentsBenchmark.fluxo__mvvm_intent       avgt    6   61.107 ± 22.624   ms/op
 *
 * IncrementIntentsBenchmark.mvicore__mvi_reducer       ss    6    8.944 ± 14.073   ms/op
 * IncrementIntentsBenchmark.ballast__mvi_handler       ss    6   20.474 ± 13.833   ms/op
 * IncrementIntentsBenchmark.mvikotlin__mvi_reducer     ss    6   21.143 ± 11.729   ms/op
 * IncrementIntentsBenchmark.orbit__mvvm_intent         ss    6   25.228 ±  3.836   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_handler         ss    6   30.315 ± 14.676   ms/op
 * IncrementIntentsBenchmark.fluxo__mvi_reducer         ss    6   37.513 ± 36.894   ms/op
 * IncrementIntentsBenchmark.fluxo__mvvm_intent         ss    6  122.150 ± 31.872   ms/op
 * ```
 */
@State(Scope.Benchmark)
@OptIn(DelicateCoroutinesApi::class)
open class IncrementIntentsBenchmark {
    private companion object {
        private const val REPETITIONS = 3_000
    }

    private enum class Intent {
        Increment,
    }


    // region Fluxo

    @Benchmark
    fun fluxo__mvvm_intent(bh: Blackhole) {
        val dispatcher = newSingleThreadContext(::fluxo__mvvm_intent.name)
        val container = container(0) {
            eventLoopContext = dispatcher
            debugChecks = false
            lazy = false
        }
        val intent: suspend StoreScope<Nothing, Int, Nothing>.() -> Unit = { updateState { it + 1 } }
        container.consumeFluxo(intent, bh, dispatcher)
    }

    @Benchmark
    fun fluxo__mvi_reducer(bh: Blackhole) {
        val dispatcher = newSingleThreadContext(::fluxo__mvi_reducer.name)
        val store = store<Intent, Int>(0, reducer = {
            when (it) {
                Intent.Increment -> this + 1
            }
        }) {
            eventLoopContext = dispatcher
            debugChecks = false
            lazy = false
        }
        store.consumeFluxo(Intent.Increment, bh, dispatcher)
    }

    @Benchmark
    fun fluxo__mvi_handler(bh: Blackhole) {
        val dispatcher = newSingleThreadContext(::fluxo__mvi_handler.name)
        val store = store<Intent, Int>(0, handler = { intent ->
            when (intent) {
                Intent.Increment -> updateState { it + 1 }
            }
        }) {
            eventLoopContext = dispatcher
            debugChecks = false
            lazy = false
        }
        store.consumeFluxo(Intent.Increment, bh, dispatcher)
    }

    private fun <I> Store<I, Int, *>.consumeFluxo(intent: I, bh: Blackhole, dispatcher: Closeable) {
        runBlocking {
            val launchDef = launchCommon(intent) { send(it) }

            consumeCommon(stateFlow, launchDef)

            @OptIn(ExperimentalFluxoApi::class)
            closeAndWait()
        }
        dispatcher.close()
        bh.consume(state)
    }

    // endregion


    @Benchmark
    fun ballast__mvi_handler(bh: Blackhole) {
        val dispatcher = newSingleThreadContext(::ballast__mvi_handler.name)
        val job = SupervisorJob()
        val vm = object : BasicViewModel<Intent, Nothing, Int>(
            coroutineScope = CoroutineScope(dispatcher + job),
            eventHandler = object : EventHandler<Intent, Nothing, Int> {
                override suspend fun EventHandlerScope<Intent, Nothing, Int>.handleEvent(event: Nothing) {}
            },
            config = BallastViewModelConfiguration.Builder().apply {
                initialState = 0
                inputStrategy = FifoInputStrategy()
                inputsDispatcher = dispatcher
                eventsDispatcher = dispatcher
                sideJobsDispatcher = dispatcher
                interceptorDispatcher = dispatcher
                inputHandler = object : InputHandler<Intent, Nothing, Int> {
                    override suspend fun InputHandlerScope<Intent, Nothing, Int>.handleInput(input: Intent) {
                        when (input) {
                            Intent.Increment -> {
                                updateState { it + 1 }
                            }
                        }
                    }
                }
            }.build(),
        ) {}

        bh.consume(runBlocking {
            val launchDef = launchCommon(Intent.Increment) { vm.send(it) }
            consumeCommon(vm.observeStates(), launchDef, job, dispatcher)
        })
    }

    @Benchmark
    fun mvicore__mvi_reducer(bh: Blackhole) {
        val feature = ReducerFeature<Intent, Int, Nothing>(
            initialState = 0,
            reducer = { state, wish ->
                when (wish) {
                    Intent.Increment -> state + 1
                }
            }
        )
        runBlocking {
            val launchDef = launchCommon(Intent.Increment) { feature.accept(it) }
            suspendCoroutine { cont ->
                feature.subscribe(observer = object : io.reactivex.Observer<Int> {
                    override fun onNext(state: Int) {
                        if (state >= REPETITIONS) {
                            cont.resume(state)
                        }
                    }

                    override fun onComplete() {}
                    override fun onError(e: Throwable) {}
                    override fun onSubscribe(d: Disposable) {}
                })
            }
            launchDef.join()
        }
        feature.dispose()
        bh.consume(feature.state)
    }

    @Benchmark
    fun mvikotlin__mvi_reducer(bh: Blackhole) {
        val store = DefaultStoreFactory().create<Intent, Int>(name = "CounterStore", initialState = 0) {
            when (it) {
                Intent.Increment -> this + 1
            }
        }
        runBlocking {
            val launchDef = launchCommon(Intent.Increment) { store.accept(it) }
            consumeCommon(store.states, launchDef)
        }
        store.dispose()
        bh.consume(store.state)
    }

    @Benchmark
    fun orbit__mvvm_intent(bh: Blackhole) {
        val dispatcher = newSingleThreadContext(::orbit__mvvm_intent.name)
        val job = SupervisorJob()
        val host = object : OrbitContainerHost<Int, Nothing> {
            override val container = CoroutineScope(dispatcher + job).orbitContainer<Int, Nothing>(0)
        }

        runBlocking {
            val intent: suspend SimpleSyntax<Int, Nothing>.() -> Unit = { reduce { state + 1 } }
            val launchDef = launchCommon(intent) { host.orbitIntent(transformer = it) }
            consumeCommon(host.container.stateFlow, launchDef, job, dispatcher)
        }

        bh.consume(host.container.stateFlow.value)
    }


    private inline fun <I> CoroutineScope.launchCommon(intent: I, crossinline send: suspend (I) -> Unit) = launch {
        @Suppress("UnnecessaryVariable")
        val i = intent
        repeat(REPETITIONS) {
            send(i)
            yield()
        }
    }

    private suspend fun consumeCommon(
        stateFlow: Flow<Int>,
        launchDef: Job,
        parentJob: Job? = null,
        dispatcher: Closeable? = null,
    ): Int {
        val state = stateFlow.first { it >= REPETITIONS }

        launchDef.join()
        parentJob?.cancelAndJoin()
        @Suppress("BlockingMethodInNonBlockingContext")
        dispatcher?.close()
        return state
    }
}
