plugins {
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.kotlinx.atomicfu)
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
            implementation(libs.kotlinx.coroutines.core)
        }
    }
    commonJs {
        test.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}

extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
    targets.named("android") {
        (this as com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget)
            .withHostTest {}
    }
    sourceSets.named("androidHostTest") {
        dependsOn(sourceSets.named("commonJvmTest").get())
    }
    sourceSets.named("androidMain") {
        dependencies {
            compileOnly(libs.androidx.annotation)
        }
    }
}
