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
    optIns = listOf(
        "kt.fluxo.core.annotation.ExperimentalFluxoApi",
        "kt.fluxo.core.annotation.InternalFluxoApi",
    ),
)
setupPublication()
setupBinaryCompatibilityValidator()

kotlin.setupSourceSets {
    // Workaround for
    // https://youtrack.jetbrains.com/issue/KT-57235#focus=Comments-27-6989130.0-0
    val js by bundle()
    js.main.dependencies {
        implementation(libs.kotlin.atomicfu.runtime)
    }
}

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
