import com.android.build.gradle.LibraryExtension
import impl.hasExtension
import js.api.KotlinJsApiBuildTask
import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.KotlinApiCompareTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val PLUGIN_ID = "org.jetbrains.kotlinx.binary-compatibility-validator"

fun Project.setupBinaryCompatibilityValidator(
    config: BinaryCompatibilityValidatorConfig? = getDefaults<BinaryCompatibilityValidatorConfig>(),
) {
    val disabledByRelease = config?.disableForNonRelease == true && !isRelease().get()
    if (disabledByRelease || disableTests().get() || !isGenericCompilationEnabled) {
        return
    }
    when {
        hasExtension<KotlinMultiplatformExtension>() -> setupBinaryCompatibilityValidatorMultiplatform(config)
        hasExtension<LibraryExtension>() -> setupBinaryCompatibilityValidatorAndroidLibrary(config)
        else -> error("Unsupported project type for API checks")
    }
}

private fun Project.setupBinaryCompatibilityValidatorMultiplatform(config: BinaryCompatibilityValidatorConfig?) {
    applyBinaryCompatibilityValidator(config)

    if (config?.jsApiChecks != false) {
        KotlinJsApiBuildTask.setupTask(project = this, multiplatformExtension)
    }

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

private fun Project.setupBinaryCompatibilityValidatorAndroidLibrary(config: BinaryCompatibilityValidatorConfig?) {
    applyBinaryCompatibilityValidator(config)
}

private fun Project.applyBinaryCompatibilityValidator(config: BinaryCompatibilityValidatorConfig?) {
    plugins.apply(PLUGIN_ID)
    config ?: return
    extensions.configure<ApiValidationExtension> {
        ignoredPackages += config.ignoredPackages
        nonPublicMarkers += config.nonPublicMarkers
        ignoredClasses += config.ignoredClasses
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
    ApiTarget.ANDROID -> isMultiplatformTargetEnabled(Target.ANDROID)
    ApiTarget.JVM -> isMultiplatformTargetEnabled(Target.JVM)
}

private enum class ApiTarget {
    ANDROID,
    JVM,
}
