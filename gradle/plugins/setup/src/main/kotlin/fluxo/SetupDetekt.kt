package fluxo

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin

fun Project.setupDetekt() {
    checkIsRootProject()

    allprojects {
        plugins.apply("io.gitlab.arturbosch.detekt")

        extensions.configure<DetektExtension> {
            parallel = true
            buildUponDefaultConfig = true
            config = files("$rootDir/detekt.yml")
        }

        tasks.register("detektAll") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            dependsOn(tasks.withType<Detekt>())
        }

        tasks.configureEach {
            if (name == "build") {
                dependsOn("detektAll")
            }
        }
    }
}
