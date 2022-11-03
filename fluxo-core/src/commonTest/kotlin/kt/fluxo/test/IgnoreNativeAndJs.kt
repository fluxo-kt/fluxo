package kt.fluxo.test

/**
 * Marks test or suite as ignored.
 */
@OptionalExpectation
@OptIn(ExperimentalMultiplatform::class)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
expect annotation class IgnoreNativeAndJs()
