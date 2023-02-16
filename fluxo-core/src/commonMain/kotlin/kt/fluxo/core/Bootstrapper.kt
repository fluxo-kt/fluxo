package kt.fluxo.core

import kt.fluxo.core.dsl.BootstrapperScope

public typealias Bootstrapper<Intent, State, SideEffect> = suspend BootstrapperScope<Intent, State, SideEffect>.() -> Unit

public typealias BootstrapperS<Intent, State> = Bootstrapper<Intent, State, in Nothing>
