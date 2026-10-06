plugins {
    alias(libs.plugins.kotlinx.kover)
    // Publication plugin: the harness configures but requires it applied here.
    alias(libs.plugins.vanniktech.mvn.publish)
    // Supply chain: per-publication Sigstore signing + CycloneDX SBOM.
    alias(libs.plugins.sigstore.sign)
    alias(libs.plugins.cyclonedx.bom)
    // Release javadoc jars (root `useDokka`). Applied in every build, not only releases, so that `./updateBaselines`
    // can run Dokka and pin its generator dependencies; a release verifies them strictly.
    alias(libs.plugins.kotlin.dokka)
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
}

extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
    // See fluxo-common/build.gradle.kts for the rationale on the lazy matching idiom.
    // JS tests need kotlinx-browser: the root build swaps out kotlin-dom-api-compat, and the coroutines test runtime
    // calls `kotlinx.browser.window` (IrLinkageError without it). Not on wasmWasi, which has no browser variant.
    sourceSets.matching { it.name == "jsTest" || it.name == "wasmJsTest" }.configureEach {
        dependencies { implementation(libs.kotlinx.browser) }
    }
    targets.matching { it.name == "android" }.configureEach {
        (this as com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget)
            .withHostTest {}
    }
}
