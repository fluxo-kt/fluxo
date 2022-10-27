package kt.fluxo.core.dsl

public typealias InputStrategyScope<Request> = suspend (request: Request) -> Unit
