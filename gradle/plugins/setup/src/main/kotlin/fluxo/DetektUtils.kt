package fluxo

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin


private data class DetektTaskDetails(
    val platform: DetektPlatform?,
    val isMain: Boolean,
    val isTest: Boolean,
    val isMetadata: Boolean,
    val taskName: String,
)

/**
 * @see org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
 * @see org.jetbrains.kotlin.konan.target.Family
 */
private enum class DetektPlatform {
    DARWIN,
    LINUX,
    WIN,
    JS,
    WASM,
    ANDROID,
    JVM,
    UNKNOWN,
}

internal fun Detekt.isDetektTaskAllowed(): Boolean = when (getDetektTaskDetails(name).platform) {
    DetektPlatform.WIN -> Compilations.isWindowsEnabled
    DetektPlatform.DARWIN -> Compilations.isDarwinEnabled
    DetektPlatform.LINUX -> Compilations.isGenericEnabled

    DetektPlatform.JVM,
    DetektPlatform.JS,
    DetektPlatform.ANDROID,
    DetektPlatform.WASM,
    -> project.isGenericCompilationEnabled

    else -> true
}

private fun getDetektTaskDetails(name: String): DetektTaskDetails {
    val parts = name.splitCamelCase()
    check(parts.isNotEmpty() && parts[0] == DetektPlugin.DETEKT_TASK_NAME) { "Unexpected detect task name: $name" }

    var list = parts.drop(1)

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

    val platform = detektPlatformFromString(list.firstOrNull())

    return DetektTaskDetails(
        platform = platform,
        isMain = isMain,
        isTest = isTest,
        isMetadata = isMetadata,
        taskName = name,
    )
}

@Suppress("CyclomaticComplexMethod")
private fun detektPlatformFromString(platform: String?): DetektPlatform? = when {
    platform.isNullOrEmpty() ||
            platform.equals("Common", ignoreCase = true) ||
            platform.equals("Native", ignoreCase = true)
    -> null

    platform.equals("Js", ignoreCase = true) -> DetektPlatform.JS
    platform.equals("Wasm", ignoreCase = true) -> DetektPlatform.WASM
    platform.equals("Android", ignoreCase = true) -> DetektPlatform.ANDROID
    platform.equals("Linux", ignoreCase = true) -> DetektPlatform.LINUX

    platform.equals("Mingw", ignoreCase = true) ||
            platform.equals("Win", ignoreCase = true) ||
            platform.equals("Windows", ignoreCase = true)
    -> DetektPlatform.WIN

    platform.equals("Jvm", ignoreCase = true) ||
            platform.equals("Jmh", ignoreCase = true) ||
            platform.equals("Java", ignoreCase = true)
    -> DetektPlatform.JVM

    platform.equals("Darwin", ignoreCase = true) ||
            platform.equals("Ios", ignoreCase = true) ||
            platform.equals("Watchos", ignoreCase = true) ||
            platform.equals("Tvos", ignoreCase = true) ||
            platform.equals("Macos", ignoreCase = true)
    -> DetektPlatform.DARWIN

    else -> DetektPlatform.UNKNOWN
}

internal fun String.splitCamelCase(limit: Int = 0): List<String> = split(CAMEL_CASE_REGEX, limit)

@Suppress("PrivatePropertyName")
private val CAMEL_CASE_REGEX = Regex("(?<![A-Z])\\B(?=[A-Z])")
