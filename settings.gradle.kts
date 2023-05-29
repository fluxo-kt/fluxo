enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    plugins {
        // Gradle Plugin to enable auto-completion and symbol resolution for all Kotlin/Native platforms.
        // Does project repo addition, but can be enabled disabled once required libs downloaded and saved
        // https://github.com/LouisCAD/CompleteKotlin/releases
        if (providers.gradleProperty("LOAD_KMM_CODE_COMPLETION").orNull.toBoolean()) {
            id("com.louiscad.complete-kotlin") version "1.1.0"
        }
    }
    includeBuild("gradle/plugins")
}

plugins {
    id("com.gradle.enterprise") version "3.13.3"
}

dependencyResolutionManagement {
    // :kotlinNodeJsSetup requires adding of project repository
    // repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev/")
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

rootProject.name = "fluxo"

// On module update, don't forget to update '.github/workflows/deps-submission.yml'!

include(":fluxo-common")
include(":fluxo-core")
include(":fluxo-data")

include(":benchmarks:jmh")
