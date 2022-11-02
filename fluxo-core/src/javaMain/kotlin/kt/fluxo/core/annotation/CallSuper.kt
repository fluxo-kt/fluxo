package kt.fluxo.core.annotation

/**
 * Denotes that any overriding methods should invoke this method as well.
 *
 *
 * Example:
 * ```
 * @CallSuper
 * public abstract void onFocusLost();
 * ```
 *
 * @see androidx.annotation.CallSuper
 */
public actual typealias CallSuper = androidx.annotation.CallSuper
