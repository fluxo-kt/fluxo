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

    // VisualFSM 3.x retains the deprecated `Transition(KClass, KClass)` constructor that lets
    // a transition declare its FROM/TO types explicitly. 4.x removed it in favour of KSP-generated
    // factories that wire `_fromState`/`_toState` reflectively; we don't apply the KSP processor
    // on this benchmark module (orthogonal scope), so we stay on the constructor pattern.
    @Suppress("DEPRECATION")
    private class IncrementTransition : Transition<VisualFsmState, VisualFsmState>(
        VisualFsmState::class, VisualFsmState::class,
    ) {
        override fun transform(state: VisualFsmState): VisualFsmState =
            state.copy(value = state.value + 1)
    }

    private class IncrementFsmFeature : Feature<VisualFsmState, IntentIncrement>(
        initialState = VisualFsmState(),
        transitionsFactory = object : TransitionsFactory<VisualFsmState, IntentIncrement> {
            override fun create(action: IntentIncrement): List<Transition<VisualFsmState, VisualFsmState>> =
                when (action) {
                    is IntentIncrement.Increment -> listOf(IncrementTransition())
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


    @Suppress("DEPRECATION")
    private class AddTransition(private val delta: Int) : Transition<VisualFsmState, VisualFsmState>(
        VisualFsmState::class, VisualFsmState::class,
    ) {
        override fun transform(state: VisualFsmState): VisualFsmState =
            state.copy(value = state.value + delta)
    }

    private class AddFsmFeature : Feature<VisualFsmState, IntentAdd>(
        initialState = VisualFsmState(),
        transitionsFactory = object : TransitionsFactory<VisualFsmState, IntentAdd> {
            override fun create(action: IntentAdd): List<Transition<VisualFsmState, VisualFsmState>> =
                when (action) {
                    is IntentAdd.Add -> listOf(AddTransition(delta = action.value))
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
