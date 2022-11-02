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
@MustBeDocumented
@OptionalExpectation
@OptIn(ExperimentalMultiplatform::class)
@Retention(AnnotationRetention.BINARY)
@Suppress("KDocUnresolvedReference")
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
public expect annotation class CallSuper()
