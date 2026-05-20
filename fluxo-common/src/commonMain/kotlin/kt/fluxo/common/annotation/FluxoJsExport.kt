package kt.fluxo.common.annotation

/**
 * Exports declarations on Kotlin/JS while staying a no-op on targets whose
 * JavaScript export support is narrower than the Fluxo public API surface.
 */
@InternalFluxoApi
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FILE,
)
@Retention(AnnotationRetention.BINARY)
public expect annotation class FluxoJsExport()
