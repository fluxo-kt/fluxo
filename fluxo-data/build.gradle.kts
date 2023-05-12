plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.lib)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.deps.guard)
    id("fluxo-setup")
}

setupMultiplatform(
    namespace = "kt.fluxo.data",
    optIns = listOf(
        "kt.fluxo.common.annotation.InternalFluxoApi",
    ),
) {
    setupSourceSets {
        commonCompileOnly(projects.fluxoCommon)
        commonCompileOnly(libs.kotlinx.coroutines.core)

        common.test.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
setupMultiplatform(namespace = "kt.fluxo.data")
setupPublication()
setupBinaryCompatibilityValidator()

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
