package kt.fluxo.test.compare.visualfsm

import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent
import ru.kontur.mobile.visualfsm.Action
import ru.kontur.mobile.visualfsm.Feature
import ru.kontur.mobile.visualfsm.State
import ru.kontur.mobile.visualfsm.Transition
import ru.kontur.mobile.visualfsm.TransitionsFactory

internal object VisualFsmBenchmark {

    private class IncrementFsmFeature : Feature<VisualFsmState, IntentIncrement>(
        initialState = VisualFsmState(),
        transitionsFactory = object : TransitionsFactory<VisualFsmState, IntentIncrement> {
            override fun create(action: IntentIncrement): List<Transition<VisualFsmState, VisualFsmState>> {
                when (action) {
                    is IntentIncrement.Increment -> {
                        return listOf(
                            @Suppress("DEPRECATION")
                            object : Transition<VisualFsmState, VisualFsmState>(VisualFsmState::class, VisualFsmState::class) {
                                override fun transform(state: VisualFsmState): VisualFsmState {
                                    return state.copy(value = state.value + 1)
                                }
                            }
                        )
                    }
                }
            }
        },
    )

    fun smReducerStaticIncrement(): Int {
        val feature = IncrementFsmFeature()
        return runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(IntentIncrement.Increment) { feature.proceed(it) }
            feature.observeState().consumeCommonBenchmark(launchDef) { it.value }
        }
    }


    private class AddFsmFeature : Feature<VisualFsmState, IntentAdd>(
        initialState = VisualFsmState(),
        transitionsFactory = object : TransitionsFactory<VisualFsmState, IntentAdd> {
            override fun create(action: IntentAdd): List<Transition<VisualFsmState, VisualFsmState>> {
                when (action) {
                    is IntentAdd.Add -> {
                        return listOf(
                            @Suppress("DEPRECATION")
                            object : Transition<VisualFsmState, VisualFsmState>(VisualFsmState::class, VisualFsmState::class) {
                                override fun transform(state: VisualFsmState): VisualFsmState {
                                    return state.copy(value = state.value + action.value)
                                }
                            }
                        )
                    }
                }
            }
        },
    )

    fun smReducerAdd(value: Int = BENCHMARK_ARG): Int {
        val feature = AddFsmFeature()
        return runBlocking {
            val launchDef = launchCommonBenchmark { feature.proceed(IntentAdd.Add(value = value)) }
            feature.observeState().consumeCommonBenchmark(launchDef) { it.value }
        }
    }


    private sealed class IntentIncrement : Action<VisualFsmState>() {
        data object Increment : IntentIncrement()
    }

    private sealed class IntentAdd : Action<VisualFsmState>() {
        data class Add(val value: Int = 1) : IntentAdd()
    }

    private data class VisualFsmState(val value: Int = 0) : State
}
