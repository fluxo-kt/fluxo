@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package kt.fluxo.core

import kt.fluxo.core.internal.FluxoIntent
import kotlin.internal.InlineOnly

/**
 * Apply this interface to anything you want to become an MVVM+ Fluxo container host.
 * Typically, it may be an Android ViewModel, but can be applied to simple presenters etc.
 */
public interface StoreHost<State, SideEffect : Any> {
    /**
     * The Fluxo [Store] instance.
     *
     * Use factory functions to easily get a [Store] instance.
     *
     * ```
     * override val store = scope.store<State, SideEffect>(initialState)
     * ```
     */
    public val store: Store<FluxoIntent<State, SideEffect>, State, SideEffect>


    // region Migration helpers

    /**
     * Helper for migration from Orbit
     */
    @InlineOnly
    @Deprecated(message = "Please use the store instead", replaceWith = ReplaceWith("store"))
    public val container: Store<FluxoIntent<State, SideEffect>, State, SideEffect> get() = store

    // endregion
}
