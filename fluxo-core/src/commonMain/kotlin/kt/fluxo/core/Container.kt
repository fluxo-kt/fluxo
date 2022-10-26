package kt.fluxo.core

/**
 * Convenience typealias for an MVVM+ Fluxo setup
 *
 * Also helps Orbit users to migrate to Fluxo.
 */
public typealias Container<State, SideEffect> = Store<FluxoIntent<State, SideEffect>, State, SideEffect>
