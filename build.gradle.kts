@file:Suppress("SpreadOperator")

import java.net.URI
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.lib) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinx.binCompatValidator) apply false
    alias(libs.plugins.kotlin.dokka) apply false
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.fluxo.bcv.js) apply false
    // `apply false`, but declared here so the publication plugin loads into the root
    // plugin classloader the harness shares — the harness is compiled `compileOnly`
    // against vanniktech and configures MavenPublishBaseExtension reflectively, so the
    // classes must be visible to it. Library modules then apply it from this classpath.
    alias(libs.plugins.vanniktech.mvn.publish) apply false
    alias(libs.plugins.fluxo.kmp.conf)
}

val isRelease by isRelease()

// Setup project defaults.
fkcSetupRaw {
    explicitApi()

    // Default KMP setup.
    defaults {
        allDefaultTargets()
    }

    projectName = "Fluxo"
    description = "Kotlin Multiplatform MVI / MVVM+ framework"
    githubProject = "fluxo-kt/fluxo"
    group = "io.github.fluxo-kt"

    // Publish all library modules to Maven Central (Central Portal) via vanniktech.
    // POM name/URL/SCM derive from projectName/githubProject/group above; license
    // defaults to Apache-2.0 (matches LICENSE). Mirrors the sibling harness config.
    enablePublication = true
    publicationConfig {
        developerId = "amal"
        developerName = "Art Shendrik"
        developerEmail = "artyom.shendrik@gmail.com"
    }

    enableSpotless = true
    enableApiValidation = true
    apiValidation {
        tsApiChecks = true
        klibValidationEnabled = true
    }

    experimentalLatestCompilation = true
    allWarningsAsErrors = true
    useIndyLambdas = isRelease
    optInInternal = true
    optIns = listOf(
        "kotlin.js.ExperimentalJsExport",
    )

    // TODO: BinaryCompatibilityValidatorConfig.disableForNonRelease = true
}

dependencies {
    kover(projects.fluxoCommon)
    kover(projects.fluxoCore)
    kover(projects.fluxoData)
}

kover {
    // TODO: Disable Kover by default to reduce performance penalty.
    //  https://github.com/Kotlin/kotlinx-kover/issues/531#issuecomment-1929483468

    val isCI by isCI()
    reports {
        total {
            xml {
                onCheck = true
                xmlFile = layout.buildDirectory.file("reports/kover-merged-report.xml")
            }
            html {
                onCheck = !isCI && isRelease
                htmlDir = layout.buildDirectory.dir("reports/kover-merged-report-html")
            }

            verify {
                onCheck = true
                rule {
                    groupBy = kotlinx.kover.gradle.plugin.dsl.GroupingEntityType.APPLICATION
                    minBound(50)
                    bound {
                        minValue = 72
                        coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                        aggregationForGroup = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                    }
                    bound {
                        minValue = 65
                        coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.INSTRUCTION
                        aggregationForGroup = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                    }
                    bound {
                        minValue = 50
                        coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH
                        aggregationForGroup = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                    }
                }
            }
        }
        filters {
            excludes {
                classes(
                    *listOfNotNull(
                        // Test classes
                        "kt.fluxo.test.*",
                        "kt.fluxo.tests.*",
                        // Inline DSL with coverage not detected in release mode.
                        if (isRelease) "kt.fluxo.core.FluxoKt*" else null,
                        if (isRelease) "kt.fluxo.core.dsl.MigrationKt*" else null,
                    ).toTypedArray(),
                )

                if (isRelease) {
                    annotatedBy(
                        // Coverage is invalid for inline and InlineOnly methods in release mode.
                        "*Inline*",
                        // No real need for a deprecated methods' coverage.
                        "*Deprecated*",
                        // JvmSynthetic used as a marker
                        // for migration helpers hidden from non-kotlin usage
                        // and not supposed for coverage.
                        "*Synthetic*",
                    )
                }
            }
        }
    }
}

// Drift gate: rejects deprecated/removed/no-op Gradle-properties keys with rationale.
// Parsing via java.util.Properties — regex would mishandle line continuations + escapes
// that real .properties files honour. `@CacheableTask` because the inputs are
// declarative; PathSensitivity.NONE because we read by file content, not path.
@CacheableTask
abstract class CheckForbiddenFlagsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val propertiesFile: RegularFileProperty

    @get:Input
    abstract val forbidden: MapProperty<String, String>

    // Keys whose value is a prefix-match — handles families like
    // `android.defaults.buildfeatures.*` (AGP 9, AGENTS.md gotcha #21) without
    // enumerating each leaf.
    @get:Input
    abstract val forbiddenPrefixes: MapProperty<String, String>

    @TaskAction
    fun verify() {
        // ISO-8859-1 is the canonical .properties encoding per java.util.Properties spec.
        val props = java.util.Properties()
        propertiesFile.get().asFile.bufferedReader(Charsets.ISO_8859_1).use { props.load(it) }
        val violations = mutableListOf<Pair<String, String>>()
        val present: Set<String> = props.stringPropertyNames()
        for ((key, rationale) in forbidden.get()) {
            if (key in present) violations += key to rationale
        }
        for ((prefix, rationale) in forbiddenPrefixes.get()) {
            for (key in present) {
                if (key.startsWith(prefix)) violations += key to rationale
            }
        }
        if (violations.isNotEmpty()) {
            val msg = buildString {
                appendLine("Forbidden Gradle properties detected in ${propertiesFile.get().asFile}:")
                appendLine()
                for ((key, rationale) in violations) {
                    appendLine("  - `$key` = `${props[key]}`")
                    appendLine("    $rationale")
                }
                appendLine()
                appendLine("Remove these keys. The rationale per key documents why each is no-op,")
                appendLine("deprecated, removed upstream, or actively harmful at the current toolchain.")
            }
            throw GradleException(msg)
        }
    }
}

val checkForbiddenFlags by tasks.registering(CheckForbiddenFlagsTask::class) {
    description = "Rejects deprecated/removed/no-op Gradle-properties keys; drift gate."
    group = "verification"
    propertiesFile.set(layout.projectDirectory.file("gradle.properties"))
    forbidden.putAll(
        mapOf(
            "kotlin.native.binary.memoryModel" to
                "Deprecated since Kotlin 1.7.20; the new MM is the only one supported in K2.x. Setting this key is a no-op that misleads readers.",
            "kotlin.mpp.androidSourceSetLayoutVersion" to
                "Only layout v2 supported in K2.x; the property is silently ignored.",
            "kotlin.compiler.preciseCompilationResultsBackup" to
                "Always-on in K2.x; the property is silently ignored.",
            "kotlin.incremental.useClasspathSnapshot" to
                "Always-on in K2.x; the property is silently ignored.",
            "kotlin.mpp.import.enableKgpDependencyResolution" to
                "Removed in K2.x; KGP's own dependency resolution is now the only path.",
            "kotlin.mpp.stability.nowarn" to
                "KMP went stable in Kotlin 1.9; the suppression flag is obsolete.",
            "org.gradle.unsafe.watch-fs" to
                "Replaced by stable `org.gradle.vfs.watch` since Gradle 7.x; the unsafe variant is removed in Gradle 9.",
            "org.gradle.configureondemand" to
                "Deprecated in Gradle 9; incompatible with the current configuration-cache invariants.",
            "org.gradle.dependency.verification.console" to
                "Dead flag without an active gradle/verification-metadata.xml; misleads readers into thinking verification is wired.",
            "android.useAndroidX" to
                "Default-on since AGP 7.x; setting it is a no-op and clutters the file.",
            "android.enableJetifier" to
                "Default-off since AGP 7.x; enabling it harms build time for ~zero benefit on a clean AndroidX codebase.",
        ),
    )
    // AGP 9 rejects global build-feature defaults — configure per-module DSL instead
    // (AGENTS.md gotcha #21). Also rejects experimental Android lint/resource flags.
    forbiddenPrefixes.putAll(
        mapOf(
            "android.defaults.buildfeatures." to
                "AGP 9 rejects global build-feature defaults; configure per-module via the `android { buildFeatures { … } }` DSL.",
            "android.experimental." to
                "Lint/resource experimental flags are not honoured under AGP 9; configure per-module DSL or drop.",
        ),
    )
}

tasks.named("check") {
    dependsOn(checkForbiddenFlags)
}

// Sigstore keyless signing relies on the GitHub Actions OIDC token issuer
// (`id-token: write`), only granted to `release.yml` on `v*` tags. On any other
// invocation (PR/snapshot/local) `sigstoreSign*Publication` fails with "Failed to
// obtain signing certificate" because no OIDC issuer is reachable. The plugin
// auto-attaches a task per MavenPublication, so disable them up-front when the
// release-publish task isn't even in the start parameters — self-aware, no env
// discipline needed (mirrors the pattern in `settings.gradle.kts`).
val isReleasePublish = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("publishAndReleaseToMavenCentral", ignoreCase = true)
}
if (!isReleasePublish) {
    subprojects {
        pluginManager.withPlugin("dev.sigstore.sign") {
            tasks.matching { it.name.startsWith("sigstoreSign") }.configureEach {
                enabled = false
            }
        }
    }
}

allprojects {
    // Exclude unused DOM API.
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.module.name == "kotlin-dom-api-compat") {
                useTarget(libs.kotlin.stdlib.js)
            }
        }
    }

    // FIXME: Setup automatically.
    plugins.withType<org.jetbrains.dokka.gradle.DokkaPlugin> {
        extensions.configure<org.jetbrains.dokka.gradle.DokkaExtension> {
            dokkaSourceSets {
                configureEach {
                    if (name.startsWith("ios")) {
                        displayName.set("ios")
                    }

                    sourceLink {
                        localDirectory.set(rootDir)
                        remoteUrl.set(URI("https://github.com/fluxo-kt/fluxo/blob/main"))
                        remoteLineSuffix.set("#L")
                    }
                }
            }
        }
    }
}
