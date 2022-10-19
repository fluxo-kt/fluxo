package kt.fluxo.core.dsl

import kt.fluxo.core.FluxoSettings
import kt.fluxo.core.StoreMvvm
import kt.fluxo.core.internal.FluxoIntent
import kt.fluxo.core.internal.StoreMvvmImpl
import kt.fluxo.core.internal.build

public inline fun <State, SideEffect : Any> store(
    initialState: State,
    configuration: FluxoSettings<*, State, SideEffect>.() -> Unit = {},
): StoreMvvm<State, SideEffect> {
    return StoreMvvmImpl(
        initialState = initialState,
        conf = FluxoSettings<FluxoIntent<State, SideEffect>, State, SideEffect>()
            .apply(configuration)
            .build(),
    )
}
