@file:Suppress("SuspiciousCollectionReassignment")

import java.net.URL

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

        // Duplicate opt-ins here as the IDE doesn't catch settings from compile tasks.
        sourceSets.all {
            languageSettings {
                optIn("kotlin.contracts.ExperimentalContracts")
                optIn("kotlin.experimental.ExperimentalObjCName")
                optIn("kotlin.js.ExperimentalJsExport")
            }
        }

        // Configure a separate test where code runs in worker thread
        // https://kotlinlang.org/docs/compiler-reference.html#generate-worker-test-runner-trw
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
    ),
    publicationConfig = run {
        val version = libs.versions.fluxo.get()
        val isSnapshot = version.endsWith("SNAPSHOT", ignoreCase = true)
        val scmTag = if (isSnapshot) scmTag().orNull ?: "main" else "v$version"
        val url = "https://github.com/fluxo-kt/fluxo-mvi"
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
            scmUrl = "scm:git:git://github.com/fluxo-kt/fluxo-mvi.git",
            scmTag = scmTag,
            developerId = "amal",
            developerName = "Artyom Shendrik",
            developerEmail = "artyom.shendrik@gmail.com",
            signingKey = signingKey(),
            signingPassword = envOrPropValue("SIGNING_PASSWORD"),
            repositoryUserName = envOrPropValue("OSSRH_USER"),
            repositoryPassword = envOrPropValue("OSSRH_PASSWORD"),
        )
    },
)

setupVerification()

// TODO: Configure universally via convenience plugin
dependencyGuard {
    configuration("classpath")
}

koverMerged {
    enable()

    val isCI by isCI()
    xmlReport {
        onCheck.set(true)
        reportFile.set(layout.buildDirectory.file("reports/kover-merged-report.xml"))
    }
    if (!isCI) {
        htmlReport {
            onCheck.set(true)
            reportDir.set(layout.buildDirectory.dir("reports/kover-merged-report-html")) // change report directory
        }
    }

    verify {
        onCheck.set(true)
        rule {
            isEnabled = true
            target = kotlinx.kover.api.VerificationTarget.ALL
            bound {
                minValue = 80
                counter = kotlinx.kover.api.CounterType.LINE
                valueType = kotlinx.kover.api.VerificationValueType.COVERED_PERCENTAGE
            }
            bound {
                minValue = 74
                counter = kotlinx.kover.api.CounterType.INSTRUCTION
                valueType = kotlinx.kover.api.VerificationValueType.COVERED_PERCENTAGE
            }
            bound {
                minValue = 63
                counter = kotlinx.kover.api.CounterType.BRANCH
                valueType = kotlinx.kover.api.VerificationValueType.COVERED_PERCENTAGE
            }
        }
    }

    filters {
        classes {
            excludes += listOf(
                // Test classes
                "kt.fluxo.test.*",
                "kt.fluxo.tests.*",
                // Inline DSL, coverage not detected properly (still everything covered!)
                "kt.fluxo.core.FluxoKt*",
                "kt.fluxo.core.dsl.MigrationKt*",
            )
        }
        annotations {
            excludes += listOf(
                // Coverage not detected properly for inline methods (still everything covered!)
                "kotlin.internal.InlineOnly",
                // JvmSynthetic used as a marker for migration helpers and inline-only DSL hidden from non-kotlin usage
                "kotlin.jvm.JvmSynthetic",
                // No need for a deprecated methods coverage
                "kotlin.Deprecated",
            )
        }
        projects {
            excludes += listOf("jmh")
        }
    }
}

if (hasProperty("buildScan")) {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

// TODO: Set universally via toml bundle
val pinnedDeps = arrayOf(
    // security recommendations
    libs.jackson.databind,
    libs.woodstox.core,
    libs.jsoup,
).map { it.get() }

allprojects {
    configurations.all {
        if (pinnedDeps.isNotEmpty()) {
            resolutionStrategy.eachDependency {
                val module = requested.module
                for (d in pinnedDeps) {
                    if (d.module == module) {
                        useVersion(d.versionConstraint.toString())
                        because("security recommendations or other considerations")
                    }
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
                        remoteUrl.set(URL("https://github.com/fluxo-kt/fluxo-mvi/blob/main"))
                        remoteLineSuffix.set("#L")
                    }
                }
            }
        }
    }
}
