@file:Suppress("SpreadOperator")

import java.net.URL

buildscript {
    dependencies {
        classpath(libs.plugin.kotlinx.atomicfu)
    }
}

plugins {
    alias(libs.plugins.android.lib) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinx.binCompatValidator) apply false
    alias(libs.plugins.kotlin.dokka) apply false
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.fluxo.bcv.js) apply false
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

    // TODO: Set developer info.
    // developerId = "amal"
    // developerName = "Artyom Shendrik"
    // developerEmail = "artyom.shendrik@gmail.com"

    enableSpotless = true

    experimentalLatestCompilation = true
    allWarningsAsErrors = true
    useIndyLambdas = isRelease
    optInInternal = true
    optIns = listOf(
        "kotlin.js.ExperimentalJsExport",
    )

    // TODO: BinaryCompatibilityValidatorConfig.disableForNonRelease = true
}

koverReport {
    dependencies {
        kover(projects.fluxoCommon)
        kover(projects.fluxoCore)
        kover(projects.fluxoData)
    }

    val isCI by isCI()
    defaults {
        xml {
            onCheck = true
            setReportFile(layout.buildDirectory.file("reports/kover-merged-report.xml"))
        }
        html {
            onCheck = !isCI && isRelease
            // change report directory
            setReportDir(layout.buildDirectory.dir("reports/kover-merged-report-html"))
        }

        verify {
            onCheck = true
            rule {
                isEnabled = true
                entity = kotlinx.kover.gradle.plugin.dsl.GroupingEntityType.APPLICATION
                minBound(50)
                bound {
                    minValue = 72
                    metric = kotlinx.kover.gradle.plugin.dsl.MetricType.LINE
                    aggregation = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                }
                bound {
                    minValue = 65
                    metric = kotlinx.kover.gradle.plugin.dsl.MetricType.INSTRUCTION
                    aggregation = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                }
                bound {
                    minValue = 50
                    metric = kotlinx.kover.gradle.plugin.dsl.MetricType.BRANCH
                    aggregation = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
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
        tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    if (name.startsWith("ios")) {
                        displayName.set("ios")
                    }

                    sourceLink {
                        localDirectory.set(rootDir)
                        @Suppress("DEPRECATION")
                        remoteUrl.set(URL("https://github.com/fluxo-kt/fluxo/blob/main"))
                        remoteLineSuffix.set("#L")
                    }
                }
            }
        }
    }
}
