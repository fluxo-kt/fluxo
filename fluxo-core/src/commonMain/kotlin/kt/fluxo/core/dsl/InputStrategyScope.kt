package kt.fluxo.core.dsl

import kt.fluxo.core.InputStrategy
import kt.fluxo.core.intercept.StoreRequest

public typealias InputStrategyScope<Intent, State> = suspend (
    queued: StoreRequest<Intent, State>,
    guardian: InputStrategy.Guardian?,
) -> Unit
