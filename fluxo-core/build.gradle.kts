import fluxo.bundle
import fluxo.dependsOn
import fluxo.setupBinaryCompatibilityValidator
import fluxo.setupMultiplatform
import fluxo.setupSourceSets

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.lib)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.kotlin.dokka)
    id("fluxo-setup")
    id("fluxo-lint")
    id("fluxo-detekt")
}
apply<kotlinx.atomicfu.plugin.gradle.AtomicFUGradlePlugin>()

setupMultiplatform()
// TODO: setupPublication()
setupBinaryCompatibilityValidator()

kotlin {
    explicitApi()

    setupSourceSets {
        val android by bundle()
        val js by bundle()
        val jsNative by bundle()
        val java by bundle()

        jsNative dependsOn common
        java dependsOn common
        js dependsOn jsNative
        javaSet dependsOn java
        android.main.dependsOn(java.main)
        nativeSet dependsOn jsNative

        all {
            languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
            languageSettings.optIn("kt.fluxo.core.annotation.ExperimentalFluxoApi")
            languageSettings.optIn("kt.fluxo.core.annotation.InternalFluxoApi")
        }

        common.main.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        common.test.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }

        java.main.dependencies {
            compileOnly(libs.androidx.annotation)
            compileOnly(libs.jsr305)
        }
        java.test.dependencies {
            implementation(libs.kotlinx.lincheck)
        }

        android.main.dependencies {
            compileOnly(libs.androidx.annotation)
            compileOnly(libs.jsr305)
        }
    }
}

android {
    buildToolsVersion = libs.versions.androidBuildTools.get()
}
