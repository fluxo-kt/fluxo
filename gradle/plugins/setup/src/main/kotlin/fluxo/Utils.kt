package fluxo

import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

internal val Project.multiplatformExtension: KotlinMultiplatformExtension
    get() = kotlinExtension as KotlinMultiplatformExtension

internal inline fun <reified T : Any> Project.hasExtension(): Boolean =
    extensions.findByType<T>() != null

fun Project.checkIsRootProject() {
    require(rootProject == this) { "Must be called on a root project" }
}
