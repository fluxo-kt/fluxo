@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    `maven-publish`
}

// Originally based on awesome gradle-setup-plugin by arkivanov
// https://github.com/arkivanov/gradle-setup-plugin/tree/master/src/main/java/com/arkivanov/gradle

dependencies {
    compileOnly(libs.plugin.android)
    compileOnly(libs.plugin.binCompatValidator)
    compileOnly(libs.plugin.dokka)
    compileOnly(libs.plugin.intellij)
    implementation(libs.detekt.core)
    implementation(libs.plugin.detekt)
}

gradlePlugin {
    plugins.create(project.name) {
        id = "fluxo-setup"
        implementationClass = "fluxo.GradleSetupPlugin"
    }
}
