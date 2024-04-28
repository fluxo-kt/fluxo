plugins {
    alias(libs.plugins.kotlinx.kover)
}

fkcSetupMultiplatform(
    namespace = "kt.fluxo.common",
) {
    commonCompileOnly(libs.kotlin.stdlib)
    jsSet.main.dependencies {
        implementation(libs.kotlin.stdlib.js)
    }
}

// Special support for switching InlineOnly on/off
// to improve code coverage calculations and debugging.
run {
    val enabled = true
    if (!enabled) return@run
    val inlineOnlySwitcher = tasks.register("inlineOnlySwitcher") {
        val isRelease = isRelease()
        val file = project.file("src/commonMain/kotlin/kt/fluxo/common/annotation/InlineOnly.kt")
        doLast {
            if (file.exists()) {
                val isOn by isRelease
                logger.lifecycle("InlineOnly is ${if (isOn) "ON" else "OFF"}")

                var content = """
                        @file:Suppress(
                            "AMBIGUOUS_EXPECTS",
                            "ACTUAL_WITHOUT_EXPECT",
                            "EXPOSED_TYPEALIAS_EXPANDED_TYPE",
                            "INVISIBLE_REFERENCE",
                        )
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
