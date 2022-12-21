package kt.fluxo.core

import kt.fluxo.core.dsl.SideJobScope

public typealias SideJob<Intent, State, SideEffect> = suspend SideJobScope<Intent, State, SideEffect>.() -> Unit
