package kt.fluxo.test

/**
 * Use [org.junit.Ignore] directly istead of [kotlin.test.Ignore] to avoid the KT-29341 error.
 */
actual typealias IgnoreJvm = org.junit.Ignore
