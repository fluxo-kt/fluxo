package kt.fluxo.common.annotation

/**
 * Specifies that this function shouldn't be called directly without inlining.
 *
 * @see kotlin.internal.InlineOnly
 */
@InternalFluxoApi
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
public expect annotation class InlineOnly()
