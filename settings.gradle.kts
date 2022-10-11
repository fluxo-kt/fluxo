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
    plugins.id("com.arkivanov.gradle.setup")
    includeBuild("gradle-setup")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "fluxo-kt"

include(":fluxo-core")
