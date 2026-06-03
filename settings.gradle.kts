@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }

    // Dogfood the in-repo harness locally; on CI — or any fresh checkout without the sibling —
    // resolve the PUBLISHED io.github.fluxo-kt.fluxo-kmp-conf plugin, so CI builds exactly what
    // external consumers get (invariant I1). Computed inside pluginManagement because that block is
    // evaluated before the settings-script body — a top-level val would be out of scope here.
    // providers.*/settingsDir keep it configuration-cache-correct under strict CC.
    val useLocalHarness =
        providers.gradleProperty("fluxo.dogfood").orNull?.toBooleanStrictOrNull()
            ?: (providers.environmentVariable("CI").orNull?.toBooleanStrictOrNull() != true &&
                settingsDir.resolveSibling("fluxo-kmp-conf").exists())
    if (useLocalHarness) {
        includeBuild("../fluxo-kmp-conf/self")
        includeBuild("../fluxo-kmp-conf")
    }
}

plugins {
    // https://plugins.gradle.org/plugin/com.gradle.develocity
    id("com.gradle.develocity") version "4.4.1"
}

dependencyResolutionManagement {
    // :kotlinNodeJsSetup requires adding of project repository
    // repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        // No snapshot repository on purpose: this build resolves only released
        // dependencies. `0.1.0-SNAPSHOT` is the version this project *publishes*
        // to the Central Portal snapshot repo, never one it consumes. (The retired
        // OSSRH snapshot repos that used to live here have 404'd since Sonatype's
        // shutdown; re-add a live repo only if a real -SNAPSHOT dependency appears.)
    }
}

rootProject.name = "fluxo"

include(":fluxo-common")
include(":fluxo-core")
include(":fluxo-data")

include(":benchmarks:jmh")
