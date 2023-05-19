@file:Suppress("SuspiciousCollectionReassignment", "SpreadOperator")

import java.net.URL

buildscript {
    dependencies {
        classpath(libs.plugin.kotlinx.atomicfu)
        classpath(libs.kotlin.gradle.api)
        classpath(libs.kotlin.atomicfu)
    }
}

plugins {
    id("fluxo-setup")
    alias(libs.plugins.android.lib) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.dokka) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinx.binCompatValidator) apply false
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.deps.guard)
    alias(libs.plugins.deps.versions)
    alias(libs.plugins.task.tree)
}

setupDefaults(
    multiplatformConfigurator = {
        // TODO: Enable the default target hierarchy:
        //  https://kotlinlang.org/docs/whatsnew1820.html#new-approach-to-source-set-hierarchy
        // @OptIn(ExperimentalKotlinGradlePluginApi::class)
        // targetHierarchy.default()

        explicitApi()

        android()
        jvm()
        if (project.isGenericCompilationEnabled) {
            js(IR) {
                // Earlier required for generation of TypeScript declaration files
                // https://kotlinlang.org/docs/js-ir-compiler.html#preview-generation-of-typescript-declaration-files-d-ts
                binaries.executable()

                compilations.all {
                    kotlinOptions {
                        // moduleKind = "es"
                        sourceMap = true
                        metaInfo = true
                    }
                }
                nodejs()
                browser()

                generateTypeScriptDefinitions()
            }
        }

        if (Compilations.isGenericEnabled) {
            linuxX64()
        }
        if (Compilations.isWindowsEnabled) {
            mingwX64()
        }
        if (Compilations.isDarwinEnabled) {
            iosCompat()
            watchosCompat()
            tvosCompat()
            macosCompat()
        }

        // Configure a separate test where code runs in worker thread
        // https://kotlinlang.org/docs/compiler-reference.html#generate-worker-test-runner-trw
        val backgroundNativeTest = false
        if (backgroundNativeTest) {
            targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests<*>>().all {
                binaries {
                    test("background", listOf(DEBUG)) {
                        freeCompilerArgs += "-trw"
                    }
                }
                testRuns {
                    @Suppress("UNUSED_VARIABLE")
                    val background by creating {
                        setExecutionSourceFrom(binaries.getTest("background", DEBUG))
                    }
                }
            }
        }
    },
    androidConfig = AndroidConfigSetup(
        minSdkVersion = libs.versions.androidMinSdk.get().toInt(),
        compileSdkVersion = libs.versions.androidCompileSdk.get().toInt(),
        targetSdkVersion = libs.versions.androidTargetSdk.get().toInt(),
        buildToolsVersion = libs.versions.androidBuildTools.get(),
        configurator = {
            if (this is com.android.build.gradle.LibraryExtension) {
                // Optimize code with R8 for android release aar
                // TODO: On-device tests for aar
                buildTypes {
                    release {
                        isMinifyEnabled = true
                        proguardFile(rootProject.file("rules.pro"))
                    }
                }
            }
        },
    ),
    kotlinConfig = KotlinConfigSetup(
        optInInternal = true,
        warningsAsErrors = true,
        alwaysIndyLambdas = false,
        addStdlibDependency = true,
        setupCoroutines = false,
        optIns = listOf(
            "kotlin.js.ExperimentalJsExport",
        ),
    ),
    publicationConfig = run {
        val version = libs.versions.fluxo.get()
        val isSnapshot = version.contains("SNAPSHOT", ignoreCase = true)
        val scmTag = if (isSnapshot) scmTag().orNull ?: "main" else "v$version"
        val url = "https://github.com/fluxo-kt/fluxo"
        val publicationUrl = "$url/tree/$scmTag"
        PublicationConfig(
            // https://central.sonatype.org/publish/publish-gradle/
            // https://central.sonatype.org/publish/publish-guide/#initial-setup
            // https://central.sonatype.org/publish/requirements/coordinates/#choose-your-coordinates
            // https://github.com/jonashackt/github-actions-release-maven
            // https://dev.to/kotlin/how-to-build-and-publish-a-kotlin-multiplatform-library-creating-your-first-library-1bp8
            group = "io.github.fluxo-kt",
            version = version,
            projectName = "Fluxo",
            projectDescription = "Kotlin Multiplatform MVI / MVVM+ framework",
            projectUrl = url,
            publicationUrl = publicationUrl,
            scmUrl = "scm:git:git://github.com/fluxo-kt/fluxo.git",
            scmTag = scmTag,
            developerId = "amal",
            developerName = "Artyom Shendrik",
            developerEmail = "artyom.shendrik@gmail.com",
            signingKey = signingKey(),
            signingPassword = envOrPropValue("SIGNING_PASSWORD"),
            repositoryUserName = envOrPropValue("OSSRH_USER"),
            repositoryPassword = envOrPropValue("OSSRH_PASSWORD"),
            project = project,
        )
    },
    binaryCompatibilityValidatorConfig = BinaryCompatibilityValidatorConfig(
        disableForNonRelease = true,
    ),
)

setupVerification()

// TODO: Configure universally via convenience plugin
dependencyGuard {
    configuration("classpath")
}

koverReport {
    dependencies {
        kover(projects.fluxoCommon)
        kover(projects.fluxoCore)
        kover(projects.fluxoData)
    }

    val isRelease by isRelease()
    val isCI by isCI()
    defaults {
        xml {
            onCheck = true
            setReportFile(layout.buildDirectory.file("reports/kover-merged-report.xml"))
        }
        html {
            onCheck = !isCI && isRelease
            setReportDir(layout.buildDirectory.dir("reports/kover-merged-report-html")) // change report directory
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
                ).toTypedArray()
            )

            if (isRelease) {
                annotatedBy(
                    // Coverage is invalid for inline and InlineOnly methods in release mode.
                    "*Inline*",
                    // No real need for a deprecated methods' coverage.
                    "*Deprecated*",
                    // JvmSynthetic used as a marker for migration helpers hidden from non-kotlin usage and not supposed for coverage.
                    "*Synthetic*",
                )
            }
        }
    }
}

if (hasProperty("buildScan")) {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

val pinnedDeps = libs.bundles.pinned.get().associate {
    it.module to it.versionConstraint.toString()
}
allprojects {
    if (pinnedDeps.isNotEmpty()) {
        configurations.all {
            resolutionStrategy.eachDependency {
                val version = pinnedDeps[requested.module]
                if (version != null) {
                    useVersion(version)
                    because("security recommendations or other considerations")
                }
            }
        }
    }

    plugins.withType<org.jetbrains.dokka.gradle.DokkaPlugin> {
        tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    if (name.startsWith("ios")) {
                        displayName.set("ios")
                    }

                    sourceLink {
                        localDirectory.set(rootDir)
                        remoteUrl.set(URL("https://github.com/fluxo-kt/fluxo/blob/main"))
                        remoteLineSuffix.set("#L")
                    }
                }
            }
        }
    }
}
