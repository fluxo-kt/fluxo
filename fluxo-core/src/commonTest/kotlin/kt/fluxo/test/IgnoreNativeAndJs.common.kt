@file:Suppress("MatchingDeclarationName")

package kt.fluxo.test

/**
 * Marks test or suite as ignored for Native and JS platforms.
 */
@OptionalExpectation
@OptIn(ExperimentalMultiplatform::class)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
expect annotation class IgnoreNativeAndJs()
