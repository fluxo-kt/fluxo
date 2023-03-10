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

@Incubating
fun Project.scmTag(): Provider<String?> {
    val envOrProp = envOrProp("SCM_TAG")
    return provider {
        var result = envOrProp.orNull
        if (result.isNullOrEmpty()) {
            result = runCommand("git tag -l --points-at HEAD") // current tag name
                ?: runCommand("git rev-parse --short=8 HEAD") // current commit short hash
                    ?: runCommand("git rev-parse --abbrev-ref HEAD") // current branch name
        }
        result
    }
}

fun Project.signingKey(): String? = envOrPropValue("SIGNING_KEY")?.replace("\\n", "\n")

@Suppress("UnstableApiUsage")
fun Project.runCommand(command: String): String? {
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
