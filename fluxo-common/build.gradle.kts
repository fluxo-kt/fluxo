plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.lib)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.deps.guard)
    id("fluxo-setup")
}

setupMultiplatform(
    namespace = "kt.fluxo.common",
    config = requireDefaultKotlinConfigSetup().copy(
        addStdlibDependency = false,
    ),
) {
    setupSourceSets {
        common.main.dependencies {
            commonCompileOnly(kotlin("stdlib"))
        }
        jsSet.main.dependencies {
            implementation(kotlin("stdlib-js"))
        }
    }
}
setupPublication()
setupBinaryCompatibilityValidator()

dependencyGuard {
    configuration("androidDebugRuntimeClasspath")
    configuration("androidReleaseRuntimeClasspath")
    configuration("debugRuntimeClasspath")
    configuration("jvmRuntimeClasspath")
    configuration("releaseRuntimeClasspath")
    if (project.isGenericCompilationEnabled) {
        configuration("jsRuntimeClasspath")
    }
}

// Special support for switching InlineOnly on/off to improve code coverage calculations and debugging.
run {
    val enabled = true
    val inlineOnlySwitcher = tasks.register("inlineOnlySwitcher") {
        doLast {
            val file = project.file("src/commonMain/kotlin/kt/fluxo/common/annotation/InlineOnly.kt")
            if (enabled && file.exists()) {
                val isOn = isRelease().get()
                logger.lifecycle("InlineOnly is ${if (isOn) "ON" else "OFF"}")

                var content = """
                        @file:Suppress("AMBIGUOUS_EXPECTS", "ACTUAL_WITHOUT_EXPECT", "EXPOSED_TYPEALIAS_EXPANDED_TYPE", "INVISIBLE_REFERENCE")

                        package kt.fluxo.common.annotation

                        /** @see kotlin.internal.InlineOnly */
                        @InternalFluxoApi
                        """.trimIndent()
                content += when {
                    isOn -> "\npublic actual typealias InlineOnly = kotlin.internal.InlineOnly\n"
                    else -> "\npublic actual annotation class InlineOnly\n"
                }
                file.writeText(content)
            }
        }
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        dependsOn(inlineOnlySwitcher)
    }
}
