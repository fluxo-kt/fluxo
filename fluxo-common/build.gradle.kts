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
    inlineOnlySourceSets.forEach { sourceSet ->
        sourceSets.named(sourceSet) {
            kotlin.srcDir(inlineOnlyGeneratedDir.map { it.dir("$sourceSet/kotlin") })
        }
    }
    (fluxoJsExportSourceSets + "jsMain").forEach { sourceSet ->
        sourceSets.named(sourceSet) {
            kotlin.srcDir(fluxoJsExportGeneratedDir.map { it.dir("$sourceSet/kotlin") })
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(inlineOnlySwitcher)
    dependsOn(fluxoJsExportSwitcher)
}
