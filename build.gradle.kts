@file:Suppress("SpreadOperator")

import java.net.URI

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
