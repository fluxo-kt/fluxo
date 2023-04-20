import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    `maven-publish`
}

// Originally based on awesome gradle-setup-plugin by arkivanov
// https://github.com/arkivanov/gradle-setup-plugin/tree/1f7ac3c/src/main/java/com/arkivanov/gradle

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget("11"))
}

dependencies {
    compileOnly(libs.plugin.kotlin)
    compileOnly(libs.plugin.android)
    compileOnly(libs.plugin.ksp)
    compileOnly(libs.plugin.binCompatValidator)
    compileOnly(libs.plugin.dokka)
    compileOnly(libs.plugin.intellij)
    compileOnly(libs.plugin.jetbrains.compose)
    implementation(libs.detekt.core)
    implementation(libs.plugin.detekt)
}

gradlePlugin {
    plugins.create(project.name) {
        id = "fluxo-setup"
        implementationClass = "GradleSetupPlugin"
    }
}
