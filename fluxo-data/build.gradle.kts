plugins {
    alias(libs.plugins.kotlinx.kover)
}

fkcSetupMultiplatform(
    namespace = "kt.fluxo.data",
    optIns = listOf(
        "kt.fluxo.common.annotation.InternalFluxoApi",
    ),
) {
    commonCompileOnly(projects.fluxoCommon)
    commonCompileOnly(libs.kotlinx.coroutines.core)
    common.test.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }
}
