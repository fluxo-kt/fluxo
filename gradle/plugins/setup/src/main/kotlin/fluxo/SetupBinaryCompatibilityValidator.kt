package fluxo

import com.android.build.gradle.LibraryExtension
import kotlinx.validation.KotlinApiCompareTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.setupBinaryCompatibilityValidator() {
    when {
        hasExtension<KotlinMultiplatformExtension>() -> setupBinaryCompatibilityValidatorMultiplatform()
        hasExtension<LibraryExtension>() -> setupBinaryCompatibilityValidatorAndroidLibrary()
        else -> error("Unsupported project type for API checks")
    }
}

private fun Project.setupBinaryCompatibilityValidatorMultiplatform() {
    plugins.apply("org.jetbrains.kotlinx.binary-compatibility-validator")

    afterEvaluate {
        tasks.withType<KotlinApiCompareTask> {
            val target = getTargetForTaskName(taskName = name)
            if (target != null) {
                enabled = isMultiplatformApiTargetAllowed(target)
                if (!enabled) {
                    println("API check $this disabled!")
                }
            }
        }
    }
}

private fun Project.setupBinaryCompatibilityValidatorAndroidLibrary() {
    if (Compilations.isGenericEnabled) {
        plugins.apply("org.jetbrains.kotlinx.binary-compatibility-validator")
    }
}

private fun getTargetForTaskName(taskName: String): ApiTarget? {
    val targetName = taskName.removeSuffix("ApiCheck").takeUnless { it == taskName } ?: return null

    return when (targetName) {
        "android" -> ApiTarget.ANDROID
        "jvm" -> ApiTarget.JVM
        else -> error("Unsupported API check task name: $taskName")
    }
}

private fun Project.isMultiplatformApiTargetAllowed(target: ApiTarget): Boolean =
    when (target) {
        ApiTarget.ANDROID -> isMultiplatformTargetEnabled(Target.ANDROID) && Compilations.isGenericEnabled
        ApiTarget.JVM -> isMultiplatformTargetEnabled(Target.JVM) && Compilations.isGenericEnabled
    }

private enum class ApiTarget {
    ANDROID,
    JVM,
}
