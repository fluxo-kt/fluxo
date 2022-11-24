@file:Suppress("SuspiciousCollectionReassignment")

import fluxo.AndroidConfig
import fluxo.ensureUnreachableTasksDisabled
import fluxo.getValue
import fluxo.iosCompat
import fluxo.isCI
import fluxo.isRelease
import fluxo.macosCompat
import fluxo.setupDefaults
import fluxo.setupVerification
import fluxo.tvosCompat
import fluxo.useK2
import fluxo.watchosCompat

buildscript {
    dependencies {
        classpath(libs.plugin.kotlinx.atomicfu)
        classpath(libs.kotlin.gradle.api)
        classpath(libs.kotlin.atomicfu)
    }
}

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("fluxo-setup")
    id("release-dependencies-diff-compare")
    id("release-dependencies-diff-create") apply false
    alias(libs.plugins.android.lib) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.dokka) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinx.binCompatValidator) apply false
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.deps.analysis)
    alias(libs.plugins.deps.versions)
    alias(libs.plugins.task.tree)
}

setupDefaults(
    multiplatformConfigurator = {
        android()
        jvm()
        js(IR) {
            nodejs()
            browser()
        }
        linuxX64()
        mingwX64()
        iosCompat()

        // Until the fix in Kotlin 1.8 https://youtrack.jetbrains.com/issue/KT-54814
        val isCi by isCI()
        if (!isCi || !libs.versions.kotlin.get().startsWith("1.7")) {
            watchosCompat()
        }

        tvosCompat()
        macosCompat()

        sourceSets.matching { it.name.endsWith("Test") }.all {
            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }

        targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests<*>>().all {
            binaries {
                // Configure a separate test where code runs in background
                test("background", listOf(DEBUG)) {
                    // https://kotlinlang.org/docs/compiler-reference.html#generate-worker-test-runner-trw
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
    },
    androidConfig = AndroidConfig(
        minSdkVersion = libs.versions.androidMinSdk.get().toInt(),
        compileSdkVersion = libs.versions.androidCompileSdk.get().toInt(),
        targetSdkVersion = libs.versions.androidTargetSdk.get().toInt(),
        buildToolsVersion = libs.versions.androidBuildTools.get(),
    ),
)

setupVerification()

ensureUnreachableTasksDisabled()

dependencyAnalysis {
    // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/wiki/Customizing-plugin-behavior
    dependencies {
        bundle("kotlin-stdlib") {
            includeGroup("org.jetbrains.kotlin")
        }
    }
    issues {
        all {
            onIncorrectConfiguration {
                severity("fail")
            }
            onUnusedDependencies {
                severity("fail")
            }
        }
    }
}

koverMerged {
    enable()

    val isCi by isCI()
    val isRelease by isRelease()
    xmlReport {
        onCheck.set(true)
        reportFile.set(layout.buildDirectory.file("reports/kover-merged-report.xml"))
    }
    if (!isCi) {
        htmlReport {
            onCheck.set(true)
            reportDir.set(layout.buildDirectory.dir("reports/kover-merged-report-html")) // change report directory
        }
    }

    if (isRelease) {
        verify {
            onCheck.set(true)
            rule {
                isEnabled = true
                target = kotlinx.kover.api.VerificationTarget.ALL
                bound {
                    minValue = 10
                    maxValue = 20
                    counter = kotlinx.kover.api.CounterType.LINE
                    valueType = kotlinx.kover.api.VerificationValueType.COVERED_PERCENTAGE
                }
            }
        }
    }
}

allprojects {
    afterEvaluate {
        // Workaround for https://youtrack.jetbrains.com/issue/KT-52776
        rootProject.extensions.findByType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>()?.apply {
            versions.webpackCli.version = "4.10.0"
        }

        val isCi by isCI()
        val isRelease by isRelease()
        val useK2 by useK2()
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            val isTestTask = "Test" in name
            kotlinOptions {
                if (!isTestTask && (isCi || isRelease)) {
                    allWarningsAsErrors = true
                }

                val javaVersion = libs.versions.javaLangTarget.get()
                jvmTarget = javaVersion
                javaParameters = true

                val kotlinVersion = libs.versions.kotlinLangVersion.get()
                languageVersion = kotlinVersion
                apiVersion = kotlinVersion

                // https://github.com/JetBrains/kotlin/blob/master/compiler/testData/cli/jvm/extraHelp.out
                freeCompilerArgs += listOf(
                    "-Xcontext-receivers",
                    "-Xjsr305=strict",
                    "-Xjvm-default=all",
                    "-Xtype-enhancement-improvements-strict-mode",
                    "-opt-in=kotlin.RequiresOptIn",
                )
                freeCompilerArgs += if (isCi || isRelease) listOf(
                    "-Xlambdas=indy",
                    "-Xsam-conversions=indy",
                    "-Xvalidate-bytecode",
                    "-Xvalidate-ir",
                ) else listOf(
                    // more data on lambdas for debugging
                    "-Xlambdas=class",
                    "-Xsam-conversions=class",
                )
                if (useK2) {
                    freeCompilerArgs += "-Xuse-k2"
                }
            }
        }

        // Remove log pollution until Android support in KMP improves.
        project.extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()?.let { kmpExt ->
            kmpExt.sourceSets.removeAll {
                setOf(
                    "androidAndroidTestRelease",
                    "androidTestFixtures",
                    "androidTestFixturesDebug",
                    "androidTestFixturesRelease",
                ).contains(it.name)
            }
        }
    }
}

subprojects {
    // Convenience task that will print full dependencies tree for any module
    // Use `buildEnvironment` task for the report about plugins
    // https://docs.gradle.org/current/userguide/viewing_debugging_dependencies.html
    tasks.register<DependencyReportTask>("allDeps")

    plugins.apply("release-dependencies-diff-create")
}
