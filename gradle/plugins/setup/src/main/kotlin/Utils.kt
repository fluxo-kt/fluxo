@file:Suppress("TooManyFunctions", "ktPropBy")

import impl.envOrProp
import impl.envOrPropFlag
import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.util.regex.Pattern

operator fun Provider<Boolean>.getValue(t: Any?, p: Any?): Boolean = orNull == true

fun Project.envOrPropValue(name: String): String? = envOrProp(name).orNull?.takeIf { it.isNotEmpty() }
fun Project.envOrPropInt(name: String): Int? = envOrPropValue(name)?.toIntOrNull()
fun Project.envOrPropList(name: String): List<String> = envOrPropValue(name)?.split(Pattern.compile("\\s*,\\s*")).orEmpty()

fun Project.isCI(): Provider<Boolean> = envOrPropFlag("CI")

fun Project.isRelease(): Provider<Boolean> = envOrPropFlag("RELEASE")

fun Project.useKotlinDebug(): Provider<Boolean> = envOrPropFlag("USE_KOTLIN_DEBUG")

fun Project.disableTests(): Provider<Boolean> = envOrPropFlag("DISABLE_TESTS")

fun Project.areComposeMetricsEnabled(): Provider<Boolean> = envOrPropFlag("COMPOSE_METRICS")

fun Project.isDesugaringEnabled(): Provider<Boolean> = envOrPropFlag("DESUGARING")

fun Project.isMaxDebugEnabled(): Provider<Boolean> = envOrPropFlag("MAX_DEBUG")

fun Project.isR8Disabled(): Provider<Boolean> = envOrPropFlag("DISABLE_R8")

fun Project?.buildNumber(): String? = "BUILD_NUMBER".let {
    return this?.envOrProp(it)?.orNull ?: System.getProperty(it, null)
}


fun Project.signingKey(): String? = envOrPropValue("SIGNING_KEY")?.replace("\\n", "\n")

fun Project?.buildNumberSuffix(default: String = "", delimiter: String = "."): String {
    val n = buildNumber()
    return if (!n.isNullOrBlank()) "$delimiter$n" else default
}


@Incubating
@Suppress("ComplexCondition", "MagicNumber")
fun Project.scmTag(allowBranch: Boolean = true): Provider<String?> {
    val envOrProp = envOrProp("SCM_TAG")
    return provider {
        var result = envOrProp.orNull
        if (result.isNullOrBlank()) {
            // current tag name
            var tagName = runCommand("git tag -l --points-at HEAD")
            if (tagName != null && tagName.length >= 2 && tagName[0] == 'v' && tagName[1].isDigit()) {
                tagName = tagName.substring(1)
            }

            result = tagName
                // current commit short hash
                ?: runCommand("git rev-parse --short=7 HEAD")
                    // current branch name
                    ?: if (allowBranch) runCommand("git rev-parse --abbrev-ref HEAD") else null
        } else if (result.length == 40) {
            // full commit hash, sha1
            result = result.substring(0, 7)
        }
        result
    }
}

@Suppress("UnstableApiUsage")
private fun Project.runCommand(command: String): String? {
    // https://docs.gradle.org/7.5.1/userguide/configuration_cache.html#config_cache:requirements:external_processes
    return try {
        val exec = providers.exec {
            commandLine(command.split("\\s".toRegex()))
        }
        val error = exec.standardError.asText.get()
        when {
            error.isEmpty() -> exec.standardOutput.asText.get().trim().ifEmpty { null }
            else -> {
                logger.error("Error running command `$command`: $error")
                null
            }
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        logger.error("Error running command `$command`: $e", e)
        null
    }
}
