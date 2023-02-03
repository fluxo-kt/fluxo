package fluxo

import com.android.build.gradle.LibraryExtension
import kotlinx.validation.KotlinApiCompareTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val PLUGIN_ID = "org.jetbrains.kotlinx.binary-compatibility-validator"

fun Project.setupBinaryCompatibilityValidator() {
    when {
        hasExtension<KotlinMultiplatformExtension>() -> setupBinaryCompatibilityValidatorMultiplatform()
        hasExtension<LibraryExtension>() -> setupBinaryCompatibilityValidatorAndroidLibrary()
        else -> error("Unsupported project type for API checks")
    }
}

private fun Project.setupBinaryCompatibilityValidatorMultiplatform() {
    plugins.apply(PLUGIN_ID)

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
    if (isGenericCompilationEnabled) {
        plugins.apply(PLUGIN_ID)
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

private fun Project.isMultiplatformApiTargetAllowed(target: ApiTarget): Boolean = when (target) {
    ApiTarget.ANDROID -> isMultiplatformTargetEnabled(Target.ANDROID) && isGenericCompilationEnabled
    ApiTarget.JVM -> isMultiplatformTargetEnabled(Target.JVM) && isGenericCompilationEnabled
}

private enum class ApiTarget {
    ANDROID,
    JVM,
}
