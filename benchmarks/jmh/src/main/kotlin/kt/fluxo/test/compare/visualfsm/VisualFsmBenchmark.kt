package kt.fluxo.test.compare.visualfsm

import kotlinx.coroutines.runBlocking
import kt.fluxo.test.compare.BENCHMARK_ARG
import kt.fluxo.test.compare.consumeCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmark
import kt.fluxo.test.compare.launchCommonBenchmarkWithStaticIntent
import ru.kontur.mobile.visualfsm.Action
import ru.kontur.mobile.visualfsm.Feature
import ru.kontur.mobile.visualfsm.GenerateTransitionsFactory
import ru.kontur.mobile.visualfsm.State
import ru.kontur.mobile.visualfsm.Transition

// VisualFSM 4 declares each transition as an inner class of its action; KSP generates the
// `Generated<Feature>TransitionsFactory` for every `@GenerateTransitionsFactory` feature (4.x removed the
// constructor that let transitions name their FROM/TO types by hand). The types must be visible to the
// generated code, so they are top-level `internal` instead of private members of the benchmark object.
internal object VisualFsmBenchmark {

    fun smReducerStaticIncrement(): Int {
        val feature = IncrementFsmFeature()
        val increment = IncrementAction.Increment()
        return runBlocking {
            val launchDef = launchCommonBenchmarkWithStaticIntent(increment) { feature.proceed(it) }
            feature.observeState().consumeCommonBenchmark(launchDef) { it.value }
        }
    }

    fun smReducerAdd(value: Int = BENCHMARK_ARG): Int {
        val feature = AddFsmFeature()
        return runBlocking {
            val launchDef = launchCommonBenchmark { feature.proceed(AddAction.Add(value)) }
            feature.observeState().consumeCommonBenchmark(launchDef) { it.value }
        }
    }
}

internal data class VisualFsmState(val value: Int = 0) : State

internal sealed class IncrementAction : Action<VisualFsmState>() {
    class Increment : IncrementAction() {
        inner class Apply : Transition<VisualFsmState, VisualFsmState>() {
            override fun transform(state: VisualFsmState) = state.copy(value = state.value + 1)
        }
    }
}

internal sealed class AddAction : Action<VisualFsmState>() {
    class Add(val delta: Int) : AddAction() {
        inner class Apply : Transition<VisualFsmState, VisualFsmState>() {
            override fun transform(state: VisualFsmState) = state.copy(value = state.value + delta)
        }
    }
}

@GenerateTransitionsFactory
internal class IncrementFsmFeature : Feature<VisualFsmState, IncrementAction>(
    initialState = VisualFsmState(),
    transitionsFactory = GeneratedIncrementFsmFeatureTransitionsFactory(),
)

@GenerateTransitionsFactory
internal class AddFsmFeature : Feature<VisualFsmState, AddAction>(
    initialState = VisualFsmState(),
    transitionsFactory = GeneratedAddFsmFeatureTransitionsFactory(),
)
