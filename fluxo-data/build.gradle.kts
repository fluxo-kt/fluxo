import fluxo.isGenericCompilationEnabled
import fluxo.setupBinaryCompatibilityValidator
import fluxo.setupMultiplatform
import fluxo.setupPublication
import fluxo.setupSourceSets

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.lib)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.deps.guard)
    id("fluxo-setup")
}

setupMultiplatform()
setupPublication()
setupBinaryCompatibilityValidator()

kotlin {
    setupSourceSets {
        all {
            languageSettings {
                optIn("kotlin.contracts.ExperimentalContracts")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }

        common.main.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        common.test.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencyGuard {
    configuration("androidDebugRuntimeClasspath")
    configuration("androidReleaseRuntimeClasspath")
    configuration("debugRuntimeClasspath")
    configuration("jvmRuntimeClasspath")
    configuration("releaseRuntimeClasspath")
    if (project.isGenericCompilationEnabled) {
        configuration("jsRuntimeClasspath")
    }
}

android.namespace = "kt.fluxo.data"
