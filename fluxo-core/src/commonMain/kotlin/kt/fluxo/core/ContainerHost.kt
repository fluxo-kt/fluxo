package kt.fluxo.core

import kotlin.js.JsExport

/**
 * Convenience typealias for a Fluxo [ContainerHost] setup with side effects disabled.
 */
public typealias ContainerHostS<State> = ContainerHost<State, Nothing>

/**
 * Apply this interface to anything you want to become an MVVM+ Fluxo container host.
 * Typically, it may be an Android ViewModel, but can be applied to simple presenters etc.
 */
@JsExport
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
