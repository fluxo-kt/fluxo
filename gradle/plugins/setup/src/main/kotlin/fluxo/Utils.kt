@file:Suppress("TooManyFunctions")

package fluxo

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import java.util.concurrent.TimeUnit

internal val Project.multiplatformExtension: KotlinMultiplatformExtension
    get() = kotlinExtension as KotlinMultiplatformExtension

internal inline fun <reified T : Any> Project.hasExtension(): Boolean =
    extensions.findByType<T>() != null

internal fun Project.checkIsRootProject() {
    require(rootProject == this) { "Must be called on a root project" }
}


operator fun Provider<Boolean>.getValue(thisRef: Any?, property: Any?): Boolean = orNull == true


private fun String?.tryAsBoolean() =
    arrayOf("true", "1", "on").any { it.equals(this, ignoreCase = true) }

@Suppress("unused")
private fun Project.booleanProperty(name: String): Provider<Boolean> {
    val provider = stringProperty(name)
    return provider {
        provider.orNull.tryAsBoolean()
    }
}

private fun Project.stringProperty(name: String): Provider<String> {
    return provider {
        var value = findProperty(name)?.toString()
        if (value.isNullOrEmpty() && '.' !in name) {
            value = findProperty("org.gradle.project.$name")?.toString()
        }
        value
    }
}

private fun Project.envVar(name: String): Provider<String?> {
    val env = providers.environmentVariable(name)
    return provider {
        env.orNull ?: System.getProperty(name)
    }
}


private fun Project.envOrPropFlag(name: String): Provider<Boolean> {
    val env = envVar(name)
    val prop = stringProperty(name)
    return provider { env.orNull != null || prop.orNull.tryAsBoolean() }
}

fun Project.envOrProp(name: String): Provider<String?> {
    val env = envVar(name)
    val prop = stringProperty(name)
    return provider { env.orNull?.takeIf { it.isNotEmpty() } ?: prop.orNull }
}

fun Project.isCI(): Provider<Boolean> = envOrPropFlag("CI")
fun Project.isRelease(): Provider<Boolean> = envOrPropFlag("RELEASE")
fun Project.useK2(): Provider<Boolean> = envOrPropFlag("USE_KOTLIN_K2")
fun Project.disableTests(): Provider<Boolean> = envOrPropFlag("DISABLE_TESTS")
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

fun Project.signingKey(): String? = envOrProp("SIGNING_KEY").orNull?.replace("\\n", "\n")


fun Project.runCommand(command: String): String? {
    return try {
        val parts = command.split("\\s".toRegex())
        val proc = ProcessBuilder(parts)
            .directory(rootDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        proc.waitFor(@Suppress("MagicNumber") 60, TimeUnit.MINUTES)
        val error = proc.errorStream.bufferedReader().readText().trim()
        when {
            error.isEmpty() -> proc.inputStream.bufferedReader().readText().trim().ifEmpty { null }
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
