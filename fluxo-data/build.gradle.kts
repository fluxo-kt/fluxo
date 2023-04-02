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
        matching { "Test" in it.name }.configureEach {
            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }

        common.main.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        common.test.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }

        // Workaround for
        // https://youtrack.jetbrains.com/issue/KT-57235#focus=Comments-27-6989130.0-0
        val js by bundle()
        js.main.dependencies {
            implementation(libs.kotlin.atomicfu.runtime)
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
