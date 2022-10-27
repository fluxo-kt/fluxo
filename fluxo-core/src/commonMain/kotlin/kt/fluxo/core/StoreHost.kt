package kt.fluxo.core

/**
 * Apply this interface to anything you want to become an MVI Fluxo container host.
 * Typically, it may be an Android ViewModel, but can be applied to simple presenters etc.
 */
public interface StoreHost<in Intent, out State, out SideEffect : Any> {
    /**
     * The Fluxo [Store] instance.
     *
     * Use factory functions to easily get a [Store] instance.
     *
     * ```
     * override val store = scope.store<Intent, State, SideEffect>(initialState)
     * ```
     */
    public val store: Store<Intent, State, SideEffect>
}

/**
 * Apply this interface to anything you want to become an MVVM+ Fluxo container host.
 * Typically, it may be an Android ViewModel, but can be applied to simple presenters etc.
 */
public interface ContainerHost<State, SideEffect : Any> {
    /**
     * The Fluxo [Container] instance.
     *
     * Use factory functions to easily get a [Container] instance.
     *
     * ```
     * override val store = scope.container<State, SideEffect>(initialState)
     * ```
     */
    public val container: Container<State, SideEffect>
}
