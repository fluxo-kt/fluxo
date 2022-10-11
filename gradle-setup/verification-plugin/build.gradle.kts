@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.dsl)
    alias(libs.plugins.detekt)
}

dependencies {
    compileOnly(libs.plugin.android)
    compileOnly(libs.plugin.detekt)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile::class.java).configureEach {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.name
}

detekt {
    buildUponDefaultConfig = true
    config.from(file("../../detekt.yml"))
}

gradlePlugin {
    plugins {
        create("fluxo-collect-sarif") {
            id = "fluxo-collect-sarif"
            implementationClass = "CollectSarifPlugin"
        }
        create("fluxo-lint") {
            id = "fluxo-lint"
            implementationClass = "LintPlugin"
        }
        create("fluxo-detekt") {
            id = "fluxo-detekt"
            implementationClass = "DetektPlugin"
        }
    }
}
