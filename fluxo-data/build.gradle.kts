plugins {
    alias(libs.plugins.kotlinx.kover)
    // Publication plugin: the harness configures but requires it applied here.
    alias(libs.plugins.vanniktech.mvn.publish)
}

fkcSetupMultiplatform(
    namespace = "kt.fluxo.data",
    optIns = listOf(
        "kt.fluxo.common.annotation.InternalFluxoApi",
    ),
    config = {
        // Maven artifactId — without it modules collide on the root projectName.
        projectName = "fluxo-data"
    },
) {
    common.main.dependencies {
        api(projects.fluxoCommon)
        api(libs.kotlinx.coroutines.core)
    }
    common.test.dependencies {
        implementation(kotlin("test"))
        implementation(libs.kotlinx.coroutines.test)
    }
    commonJs.test.dependencies {
        implementation(libs.kotlinx.browser)
    }
}

extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
    targets.named("android") {
        (this as com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget)
            .withHostTest {}
    }
}
