package kt.fluxo.test

/**
 * Marks test or suite as ignored for JVM/Android platforms.
 */
@OptionalExpectation
@OptIn(ExperimentalMultiplatform::class)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
expect annotation class IgnoreJvm()
