plugins {
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.kotlinx.atomicfu)
    // Applied here (not `apply false`): the harness configures but never applies the
    // publication plugin, so it must already be on this library module's classpath.
    alias(libs.plugins.vanniktech.mvn.publish)
    // Supply-chain hardening. Sigstore auto-attaches to all MavenPublications
    // and emits `.sigstore.json` bundles alongside JARs at publish time.
    alias(libs.plugins.sigstore.sign)
    // CycloneDX produces the `cyclonedxBom` task that emits a JSON SBOM of direct +
    // transitive deps. Release workflow uploads it as a GH Release asset.
    alias(libs.plugins.cyclonedx.bom)
    // Release javadoc jars (root `useDokka`). Applied in every build, not only releases, so that `./updateBaselines`
    // can run Dokka and pin its generator dependencies; a release verifies them strictly.
    alias(libs.plugins.kotlin.dokka)
}

fkcSetupMultiplatform(
    namespace = "kt.fluxo.core",
    optIns = listOf(
        "kt.fluxo.common.annotation.ExperimentalFluxoApi",
        "kt.fluxo.common.annotation.InternalFluxoApi",
    ),
    config = {
        // Maven artifactId for this module (coordinates(group, projectName, version)).
        // Without it, all modules inherit the root projectName "Fluxo" and collide on
        // a single coordinate; this pins the published `io.github.fluxo-kt:fluxo-core`.
        projectName = "fluxo-core"
        setupCoroutines = true
        setupDependencies = true
        addStdlibDependency = true
    },
) {
    common {
        main.dependencies {
            api(projects.fluxoCommon)
            // Public types extend coroutines types (Store is a StateFlow and a CoroutineScope), so consumers need it on
            // their compile classpath; the harness's setupCoroutines adds it only as an implementation dependency.
            api(libs.kotlinx.coroutines.core)
        }
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
    sourceSets.matching { it.name == "androidHostTest" }.configureEach {
        dependsOn(sourceSets.named("commonJvmTest").get())
    }
    sourceSets.matching { it.name == "androidMain" }.configureEach {
        dependencies {
            compileOnly(libs.androidx.annotation)
        }
    }
}
