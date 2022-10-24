package kt.fluxo.core.internal

import kt.fluxo.core.dsl.StoreScope

internal typealias FluxoIntent<State, SideEffect> = StoreScope<Nothing, State, SideEffect>.() -> Unit
