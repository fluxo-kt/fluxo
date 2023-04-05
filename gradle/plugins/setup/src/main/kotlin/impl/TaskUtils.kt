package impl

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin
import isGenericCompilationEnabled
import org.gradle.api.Project
import org.gradle.api.Task


internal fun Detekt.isDetektTaskAllowed(): Boolean = getTaskDetailsFromName(name).platform.isTaskAllowed(project)

internal fun Task.isTaskAllowedBasedByName(): Boolean {
    return getTaskDetailsFromName(name, allowNonDetekt = true).platform.isTaskAllowed(project)
}

private fun DetectedTaskPlatform?.isTaskAllowed(project: Project): Boolean = when (this) {
    DetectedTaskPlatform.WIN -> Compilations.isWindowsEnabled
    DetectedTaskPlatform.DARWIN -> Compilations.isDarwinEnabled
    DetectedTaskPlatform.LINUX -> Compilations.isGenericEnabled

    DetectedTaskPlatform.JVM,
    DetectedTaskPlatform.JS,
    DetectedTaskPlatform.ANDROID,
    DetectedTaskPlatform.WASM,
    -> project.isGenericCompilationEnabled

    else -> true
}

private fun getTaskDetailsFromName(name: String, allowNonDetekt: Boolean = false): DetectedTaskDetails {
    val parts = name.splitCamelCase()
    var list = if (!allowNonDetekt) {
        check(parts.isNotEmpty() && parts[0] == DetektPlugin.DETEKT_TASK_NAME) { "Unexpected detect task name: $name" }
        parts.drop(1)
    } else {
        parts
    }

    val last = parts.lastOrNull()
    val isTest = last.equals("Test", ignoreCase = true)
    val isMain = !isTest && last.equals("Main", ignoreCase = true)
    if (isTest || isMain) {
        list = list.dropLast(1)
    }

    val isMetadata = list.firstOrNull().equals("Metadata", ignoreCase = true)
    if (isMetadata) {
        list = list.drop(1)
    }

    var platform = detectPlatformFromString(list.firstOrNull())

    // Android build tasks
    @Suppress("ComplexCondition")
    if (platform == DetectedTaskPlatform.UNKNOWN &&
        list.firstOrNull().equals("assemble", ignoreCase = true) &&
        list.lastOrNull().let { it.equals("release", ignoreCase = true) || it.equals("debug", ignoreCase = true) }
    ) {
        println("// // Android assemble: $name")
        platform = DetectedTaskPlatform.ANDROID
    }

    return DetectedTaskDetails(
        platform = platform,
        isMain = isMain,
        isTest = isTest,
        isMetadata = isMetadata,
        taskName = name,
    )
}

@Suppress("CyclomaticComplexMethod")
private fun detectPlatformFromString(platform: String?): DetectedTaskPlatform? = when {
    platform.isNullOrEmpty() ||
            platform.equals("Common", ignoreCase = true) ||
            platform.equals("Native", ignoreCase = true)
    -> null

    platform.equals("Js", ignoreCase = true) -> DetectedTaskPlatform.JS
    platform.equals("Wasm", ignoreCase = true) -> DetectedTaskPlatform.WASM
    platform.equals("Linux", ignoreCase = true) -> DetectedTaskPlatform.LINUX

    platform.equals("Android", ignoreCase = true) ||
            platform.equals("bundle", ignoreCase = true) // Android AAR build tasks
    -> DetectedTaskPlatform.ANDROID

    platform.equals("Mingw", ignoreCase = true) ||
            platform.equals("Win", ignoreCase = true) ||
            platform.equals("Windows", ignoreCase = true)
    -> DetectedTaskPlatform.WIN

    platform.equals("Jvm", ignoreCase = true) ||
            platform.equals("Jmh", ignoreCase = true) ||
            platform.equals("Dokka", ignoreCase = true) ||
            platform.equals("Java", ignoreCase = true)
    -> DetectedTaskPlatform.JVM

    platform.equals("Darwin", ignoreCase = true) ||
            platform.equals("Apple", ignoreCase = true) ||
            platform.equals("Ios", ignoreCase = true) ||
            platform.equals("Watchos", ignoreCase = true) ||
            platform.equals("Tvos", ignoreCase = true) ||
            platform.equals("Macos", ignoreCase = true)
    -> DetectedTaskPlatform.DARWIN

    else -> DetectedTaskPlatform.UNKNOWN
}


private data class DetectedTaskDetails(
    val platform: DetectedTaskPlatform?,
    val isMain: Boolean,
    val isTest: Boolean,
    val isMetadata: Boolean,
    val taskName: String,
)

/**
 * @see org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
 * @see org.jetbrains.kotlin.konan.target.Family
 */
private enum class DetectedTaskPlatform {
    DARWIN,
    LINUX,
    WIN,
    JS,
    WASM,
    ANDROID,
    JVM,
    UNKNOWN,
}


internal fun String.splitCamelCase(limit: Int = 0): List<String> = split(CAMEL_CASE_REGEX, limit)

@Suppress("PrivatePropertyName")
private val CAMEL_CASE_REGEX = Regex("(?<![A-Z])\\B(?=[A-Z])")
