package kt.fluxo.core.annotation

/**
 * Marks declarations that are **internal** in Fluxo, and should not be used outside the library.
 * Their signatures and semantics may change between future releases without any warnings and without providing any migration aids.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS, AnnotationTarget.PROPERTY)
@RequiresOptIn(
    message = "This is an internal Fluxo API and should not be used from outside. No compatibility guarantees are provided.",
    level = RequiresOptIn.Level.ERROR,
)
public annotation class InternalFluxoApi
