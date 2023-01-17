package kt.fluxo.core.annotation

/**
 * The annotation should be applied to overridable non-abstract method.
 * Indicates that all the overriders must invoke this method via superclass method invocation expression.
 *
 *
 * Example:
 * ```
 * @CallSuper
 * public abstract void onFocusLost();
 * ```
 *
 * @see androidx.annotation.CallSuper
 * @see org.jetbrains.annotations.MustBeInvokedByOverriders
 */
public actual typealias CallSuper = androidx.annotation.CallSuper
