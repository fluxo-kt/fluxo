@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }

    // For local development.
    includeBuild("../fluxo-kmp-conf/self")
    includeBuild("../fluxo-kmp-conf")
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
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

rootProject.name = "fluxo"

// On module update, don't forget to update '.github/workflows/build.yml'!

include(":fluxo-common")
include(":fluxo-core")
include(":fluxo-data")

include(":benchmarks:jmh")
