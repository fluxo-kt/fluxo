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
        val js by bundle()
        val jsNative by bundle()
        val native by bundle()
        val java by bundle()

        jsNative dependsOn common
        native dependsOn common
        java dependsOn common
        js dependsOn jsNative
        javaSet dependsOn java
        nativeSet dependsOn jsNative
        nativeSet dependsOn native

        all {
            languageSettings {
                optIn("kotlin.contracts.ExperimentalContracts")
                optIn("kotlin.experimental.ExperimentalTypeInference")
                optIn("kt.fluxo.core.annotation.ExperimentalFluxoApi")
                optIn("kt.fluxo.core.annotation.InternalFluxoApi")
            }
        }

        common.main.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        common.test.dependencies {
            implementation(kotlin("reflect"))
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.datetime)
            implementation(libs.test.turbine)
        }

        java.main.dependencies {
            compileOnly(libs.androidx.annotation)
            compileOnly(libs.jsr305)
        }
    }
}
