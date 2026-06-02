plugins {
    alias(libs.plugins.kotlinx.kover)
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
    targets.named("android") {
        (this as com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget)
            .withHostTest {}
    }
    // Register generated sources via the producing task providers so the task dependency is
    // intrinsic to the source directory — every consumer (Kotlin compile, AGP's ART-profile task,
    // …) inherits it. A bare layout-dir provider carries no producer, which made Gradle 9 fail
    // ProcessLibraryArtProfileTask with an implicit-dependency error (it scans the generated
    // androidMain root for a sibling `baselineProfiles` dir).
    inlineOnlySourceSets.forEach { sourceSet ->
        sourceSets.named(sourceSet) {
            kotlin.srcDir(inlineOnlySwitcher.map { it.destinationDir.resolve("$sourceSet/kotlin") })
        }
    }
    (fluxoJsExportSourceSets + "jsMain").forEach { sourceSet ->
        sourceSets.named(sourceSet) {
            kotlin.srcDir(fluxoJsExportSwitcher.map { it.destinationDir.resolve("$sourceSet/kotlin") })
        }
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
