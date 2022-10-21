dependencies {
    compileOnly(libs.plugin.android)
    compileOnly(libs.plugin.detekt)
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
