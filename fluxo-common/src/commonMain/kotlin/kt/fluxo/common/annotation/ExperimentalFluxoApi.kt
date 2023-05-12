package kt.fluxo.common.annotation

/**
 * Marks **experimental** parts in Fluxo API. Such APIs may be changed in the future without notice.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.VALUE_PARAMETER,
)
@RequiresOptIn(
    message = "This is experimental Fluxo API. It may be changed in the future without notice.",
    level = RequiresOptIn.Level.WARNING,
)
public annotation class ExperimentalFluxoApi
