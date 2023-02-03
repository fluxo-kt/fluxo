package fluxo

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

fun Project.setupJvmApp(action: KotlinJvmProjectExtension.() -> Unit = {}) {
    extensions.configure<KotlinJvmProjectExtension> {
        disableCompilationsOfNeeded(project)
        action()
    }
}
