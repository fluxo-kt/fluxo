@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlin.dsl) apply false
    alias(libs.plugins.detekt) apply false
}

val buildTask = tasks.register("buildPlugins")

subprojects {
    buildTask.configure { dependsOn(tasks.named("build")) }

    beforeEvaluate {
        plugins.apply(libs.plugins.kotlin.dsl.get().pluginId)
        plugins.apply(libs.plugins.detekt.get().pluginId)

        dependencies {
            add("compileOnly", libs.plugin.kotlin)
        }

        tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile::class.java).configureEach {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_11.name
        }

        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension>("detekt") {
            buildUponDefaultConfig = true
            config.from(file("../../../detekt.yml"))
        }
    }
}
