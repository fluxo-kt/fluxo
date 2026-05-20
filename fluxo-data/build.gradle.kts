plugins {
    alias(libs.plugins.kotlinx.kover)
}

fkcSetupMultiplatform(
    namespace = "kt.fluxo.data",
    optIns = listOf(
        "kt.fluxo.common.annotation.InternalFluxoApi",
    ),
) {
    common.main.dependencies {
        api(projects.fluxoCommon)
        api(libs.kotlinx.coroutines.core)
    }
    common.test.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }
}

extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
    targets.named("android") {
        (this as com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget)
            .withHostTest {}
    }
}
