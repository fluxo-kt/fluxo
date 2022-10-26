package kt.fluxo.core

import kt.fluxo.core.dsl.StoreScope

public typealias FluxoIntent<State, SideEffect> = suspend StoreScope<Nothing, State, SideEffect>.() -> Unit
