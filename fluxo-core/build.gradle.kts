plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.lib)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.deps.guard)
    id("fluxo-setup")
}
apply<kotlinx.atomicfu.plugin.gradle.AtomicFUGradlePlugin>()

setupMultiplatform(
    namespace = "kt.fluxo.core",
    config = requireDefaultKotlinConfigSetup().copy(
        setupCoroutines = true,
    ),
    optIns = listOf(
        "kt.fluxo.common.annotation.ExperimentalFluxoApi",
        "kt.fluxo.common.annotation.InternalFluxoApi",
    ),
) {
    setupSourceSets {
        common.main.dependencies {
            api(projects.fluxoCommon)
        }
    }
}
setupPublication()
setupBinaryCompatibilityValidator()

// Overcome versions conflict
dependencies.constraints {
    implementation(libs.jetbrains.annotation)
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
