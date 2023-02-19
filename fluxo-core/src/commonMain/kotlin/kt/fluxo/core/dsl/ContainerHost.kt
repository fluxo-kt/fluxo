package kt.fluxo.core.dsl

import kt.fluxo.core.Container
import kotlin.js.JsExport

/**
 * Convenience typealias for a Fluxo [ContainerHost] setup with side effects off.
 */
public typealias ContainerHostS<State> = ContainerHost<State, Nothing>

/**
 * Apply this interface to anything you want to become an MVVM+ Fluxo container host.
 * Typically, it may be an Android ViewModel, or any presenters and so on.
 */
@JsExport
public interface ContainerHost<State, SideEffect : Any> {
    /**
     * The Fluxo [Container] instance.
     *
     * Use [factory functions][kt.fluxo.core.container] to get a [Container] instance.
     *
     * ```
     * override val store = scope.container<State, SideEffect>(initialState)
     * ```
     */
    public val container: Container<State, SideEffect>
}
