import fluxo.setupBinaryCompatibilityValidator
import fluxo.setupMultiplatform
import fluxo.setupPublication

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.lib)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.kotlin.dokka)
    id("fluxo-setup")
}

setupMultiplatform()
setupPublication()
setupBinaryCompatibilityValidator()
