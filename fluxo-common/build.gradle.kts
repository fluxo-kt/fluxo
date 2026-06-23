plugins {
    alias(libs.plugins.kotlinx.kover)
    // Publication plugin: the harness configures but requires it applied here.
    alias(libs.plugins.vanniktech.mvn.publish)
    // Supply-chain (R5.3): per-publication Sigstore signing + CycloneDX SBOM.
    alias(libs.plugins.sigstore.sign)
    alias(libs.plugins.cyclonedx.bom)
}

val inlineOnlyGeneratedDir = layout.buildDirectory.dir("generated/inlineOnlySwitcher")
val inlineOnlyPackageDir = "kt/fluxo/common/annotation"
val inlineOnlySourceSets = listOf("androidMain", "jvmMain", "nonJvmMain")
val fluxoJsExportGeneratedDir = layout.buildDirectory.dir("generated/fluxoJsExport")
val fluxoJsExportSourceSets = listOf("androidMain", "jvmMain", "nativeMain", "wasmJsMain")
val inlineOnlyNoOpContent = """
            package kt.fluxo.common.annotation

            /** @see kotlin.internal.InlineOnly */
            @InternalFluxoApi
            @Target(
                AnnotationTarget.FUNCTION,
                AnnotationTarget.PROPERTY,
                AnnotationTarget.PROPERTY_GETTER,
                AnnotationTarget.PROPERTY_SETTER,
            )
            @Retention(AnnotationRetention.BINARY)
            public actual annotation class InlineOnly
        """.trimIndent()
val fluxoJsExportNoOpContent = """
            package kt.fluxo.common.annotation

            @InternalFluxoApi
            @Target(
                AnnotationTarget.CLASS,
                AnnotationTarget.PROPERTY,
                AnnotationTarget.FUNCTION,
                AnnotationTarget.FILE,
            )
            @Retention(AnnotationRetention.BINARY)
            public actual annotation class FluxoJsExport
        """.trimIndent()
val fluxoJsExportJsContent = """
            @file:OptIn(kotlin.js.ExperimentalJsExport::class)

            package kt.fluxo.common.annotation

            @InternalFluxoApi
            public actual typealias FluxoJsExport = kotlin.js.JsExport
        """.trimIndent()
val inlineOnlySwitcher = tasks.register<Sync>("inlineOnlySwitcher") {
    val isRelease = isRelease()
    inputs.property("release", isRelease)
    into(inlineOnlyGeneratedDir)
    from(resources.text.fromString(inlineOnlyNoOpContent)) {
        rename { "InlineOnly.kt" }
        into("androidMain/kotlin/$inlineOnlyPackageDir")
    }
    from(resources.text.fromString(inlineOnlyNoOpContent)) {
        rename { "InlineOnly.kt" }
        into("jvmMain/kotlin/$inlineOnlyPackageDir")
    }
    from(resources.text.fromString(inlineOnlyNoOpContent)) {
        rename { "InlineOnly.kt" }
        into("nonJvmMain/kotlin/$inlineOnlyPackageDir")
    }
}
val fluxoJsExportSwitcher = tasks.register<Sync>("fluxoJsExportSwitcher") {
    into(fluxoJsExportGeneratedDir)
    fluxoJsExportSourceSets.forEach { sourceSet ->
        from(resources.text.fromString(fluxoJsExportNoOpContent)) {
            rename { "FluxoJsExport.kt" }
            into("$sourceSet/kotlin/$inlineOnlyPackageDir")
        }
    }
    from(resources.text.fromString(fluxoJsExportJsContent)) {
        rename { "FluxoJsExport.kt" }
        into("jsMain/kotlin/$inlineOnlyPackageDir")
    }
}

fkcSetupMultiplatform(
    namespace = "kt.fluxo.common",
    config = {
        // Maven artifactId — without it modules collide on the root projectName.
        projectName = "fluxo-common"
        setupCoroutines = false
    },
) {
    common.main.dependencies {
        implementation(libs.kotlin.stdlib)
    }
    jsSet.main.dependencies {
        implementation(libs.kotlin.stdlib.js)
    }
}

extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
    // Lazy + absence-tolerant: harness target-split variants (e.g. windows split_targets)
    // exclude the android target, and `targets.named("android")` would throw at config time.
    // `matching { }.configureEach { }` is a no-op when the target isn't registered.
    targets.matching { it.name == "android" }.configureEach {
        (this as com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget)
            .withHostTest {}
    }
    // Register generated sources via the producing task providers so the task dependency is
    // intrinsic to the source directory — every consumer (Kotlin compile, AGP's ART-profile task,
    // …) inherits it. A bare layout-dir provider carries no producer, which made Gradle 9 fail
    // ProcessLibraryArtProfileTask with an implicit-dependency error (it scans the generated
    // androidMain root for a sibling `baselineProfiles` dir).
    // `matching { }.configureEach { }` is lazy and absence-tolerant — entries in the lists
    // (e.g. `androidMain`, `nativeMain`) may be missing under harness target-split CI variants.
    val inlineOnlyNames = inlineOnlySourceSets.toSet()
    sourceSets.matching { it.name in inlineOnlyNames }.configureEach {
        kotlin.srcDir(inlineOnlySwitcher.map { it.destinationDir.resolve("$name/kotlin") })
    }
    val fluxoJsExportNames = (fluxoJsExportSourceSets + "jsMain").toSet()
    sourceSets.matching { it.name in fluxoJsExportNames }.configureEach {
        kotlin.srcDir(fluxoJsExportSwitcher.map { it.destinationDir.resolve("$name/kotlin") })
    }
}

// Kotlin compile gets the switcher dependency intrinsically via the srcDir task providers above.
// AGP's ProcessLibraryArtProfileTask additionally scans each android source root for a sibling
// `baselineProfiles` dir, deriving that path as a plain File that loses the builtBy — so Gradle 9
// strict validation fails unless the producer is declared for it explicitly. Match by the stable
// AGP task-name suffix rather than coupling to AGP-internal task types.
tasks.matching { it.name.endsWith("ArtProfile") }.configureEach {
    dependsOn(inlineOnlySwitcher, fluxoJsExportSwitcher)
}
