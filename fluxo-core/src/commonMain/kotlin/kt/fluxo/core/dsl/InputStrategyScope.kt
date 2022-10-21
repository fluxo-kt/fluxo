package kt.fluxo.core.dsl

import kt.fluxo.core.intercept.StoreRequest

public typealias InputStrategyScope<Intent, State> = suspend (request: StoreRequest<Intent, State>) -> Unit
