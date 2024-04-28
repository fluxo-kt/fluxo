plugins {
    alias(libs.plugins.kotlinx.kover)
    id("kotlinx-atomicfu")
}

fkcSetupMultiplatform(
    namespace = "kt.fluxo.core",
    optIns = listOf(
        "kt.fluxo.common.annotation.ExperimentalFluxoApi",
        "kt.fluxo.common.annotation.InternalFluxoApi",
    ),
    config = {
        setupCoroutines = true
        setupDependencies = true
        addStdlibDependency = true
    },
) {
    common {
        main.dependencies {
            api(projects.fluxoCommon)
        }
        test.dependencies {
            implementation(libs.kotlinx.coroutines.core.latest)
        }
    }
}
