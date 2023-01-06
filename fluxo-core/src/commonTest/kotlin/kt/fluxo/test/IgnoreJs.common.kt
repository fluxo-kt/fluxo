@file:Suppress("MatchingDeclarationName")

package kt.fluxo.test

/**
 * Marks test or suite as ignored for JS platform.
 */
@OptionalExpectation
@OptIn(ExperimentalMultiplatform::class)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
expect annotation class IgnoreJs()
