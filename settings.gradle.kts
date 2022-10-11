enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("VERSION_ORDERING_V2")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    resolutionStrategy.eachPlugin {
        if (requested.id.toString() == "com.arkivanov.gradle.setup") {
            // https://github.com/arkivanov/gradle-setup-plugin/tree/master/src/main/java/com/arkivanov/gradle
            useModule("com.github.arkivanov:gradle-setup-plugin:60ac46054c")
        }
    }
    plugins {
        id("com.arkivanov.gradle.setup")

        // Gradle Plugin to enable auto-completion and symbol resolution for all Kotlin/Native platforms.
        // Does project repo addition, but can be enabled disabled once required libs downloaded and saved
        // https://github.com/LouisCAD/CompleteKotlin/releases
        if (providers.gradleProperty("LOAD_KMM_CODE_COMPLETION").orNull.toBoolean()) {
            id("com.louiscad.complete-kotlin") version "1.1.0"
        }
    }
    includeBuild("gradle-setup")
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "fluxo-kt"

include(":fluxo-core")
