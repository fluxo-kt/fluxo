@file:Suppress("MatchingDeclarationName")

package kt.fluxo.test

/**
 * Marks test or suite as ignored for Linux platform.
 */
@OptionalExpectation
@OptIn(ExperimentalMultiplatform::class)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
expect annotation class IgnoreLinux()
