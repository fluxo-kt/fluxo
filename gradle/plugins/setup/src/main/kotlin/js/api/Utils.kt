package js.api

import impl.isTestRelated
import js.api.KotlinJsApiBuildTask.Companion.EXT
import kotlinx.validation.API_DIR
import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.KotlinApiCompareTask
import kotlinx.validation.task
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import java.io.File


private val Project.apiValidationExtensionOrNull: ApiValidationExtension?
    get() {
        val clazz: Class<*> = try {
            ApiValidationExtension::class.java
        } catch (_: Throwable) {
            return null
        }
        return generateSequence(this) { it.parent }
            .map { it.extensions.findByType(clazz) }
            .firstOrNull { it != null } as ApiValidationExtension?
    }

private fun apiCheckEnabled(projectName: String, extension: ApiValidationExtension?): Boolean {
    return extension == null || projectName !in extension.ignoredProjects && !extension.validationDisabled
}

/**
 *
 * @see kotlinx.validation.DirConfig
 */
private enum class DirConfig {
    /**
     * `api` directory for .api files.
     * Used in single target projects.
     */
    COMMON,

    /**
     * Target-based directory, used in multitarget setups.
     * E.g. for the project with targets jvm and android,
     * the resulting paths will be
     * `/api/jvm/project.api` and `/api/android/project.api`.
     */
    TARGET_DIR,
}

/**
 *
 * @see kotlinx.validation.TargetConfig
 */
private class TargetConfig(
    project: Project,
    val targetName: String? = null,
    private val dirConfig: Provider<DirConfig>? = null,
) {
    private val apiDirProvider = project.provider { API_DIR }

    fun apiTaskName(suffix: String) = when (targetName) {
        null, "" -> "api$suffix"
        else -> "${targetName}Api$suffix"
    }

    val apiDir
        get() = dirConfig?.map { dirConfig ->
            when (dirConfig) {
                DirConfig.COMMON -> API_DIR
                else -> "$API_DIR/$targetName"
            }
        } ?: apiDirProvider
}


/**
 *
 * @see kotlinx.validation.BinaryCompatibilityValidatorPlugin.configureMultiplatformPlugin
 */
internal fun Project.configureJsApiTasks(
    kotlin: KotlinMultiplatformExtension,
) {
    val extension = apiValidationExtensionOrNull
    if (!apiCheckEnabled(name, extension)) {
        return
    }

    // Common BCV tasks for multiplatform
    val commonApiDump = tasks.named("apiDump")
    val commonApiCheck = tasks.named("apiCheck")

    // Follow the strategy of BCV plugin.
    // API is not overrided as extension is different.
    val jvmTargetCountProvider = provider {
        kotlin.targets.count {
            it.platformType in arrayOf(
                KotlinPlatformType.jvm,
                KotlinPlatformType.androidJvm,
            )
        }
    }
    val dirConfig = jvmTargetCountProvider.map {
        if (it == 1) DirConfig.COMMON else DirConfig.TARGET_DIR
    }

    kotlin.targets.withType<KotlinJsIrTarget>().configureEach {
        val targetConfig = TargetConfig(project, this.name, dirConfig)
        compilations.matching { it.name == "main" && !it.isTestRelated() }.configureEach {
            configureKotlinCompilation(this, extension, targetConfig, commonApiDump, commonApiCheck)
        }
    }
}

private fun Project.configureKotlinCompilation(
    compilation: KotlinJsIrCompilation,
    extension: ApiValidationExtension?,
    targetConfig: TargetConfig,
    commonApiDump: TaskProvider<Task>? = null,
    commonApiCheck: TaskProvider<Task>? = null,
) {
    val projectName = project.name
    val apiDirProvider = targetConfig.apiDir
    val apiBuildDir = apiDirProvider.map { buildDir.resolve(it) }

    @Suppress("INVISIBLE_MEMBER")
    val binaries = compilation.binaries
        .withType<JsIrBinary>()
        .matching { it.generateTs && it.mode == KotlinJsBinaryMode.PRODUCTION }

    if (binaries.isEmpty()) {
        logger.warn(
            "Please, enable TS definitions with `generateTypeScriptDefinitions()` for :${project.name}, $compilation. " +
                    "No production binaries found with TS definitions enabled. " +
                    "Kotlin JS API verification is not possible."
        )
    }

    val apiBuild = project.tasks.register(targetConfig.apiTaskName("Build"), KotlinJsApiBuildTask::class.java) {
        // Do not enable task for empty umbrella modules
        isEnabled = apiCheckEnabled(projectName, extension) &&
                compilation.allKotlinSourceSets.any { it.kotlin.srcDirs.any { d -> d.exists() } }

        val linkTasks: Provider<Set<KotlinJsIrLink>> = provider {
            binaries.mapTo(LinkedHashSet()) { it.linkTask.get() }
        }
        val definitions: Provider<Set<File>> = linkTasks.flatMap {
            provider {
                it.flatMapTo(LinkedHashSet()) {
                    it.destinationDirectory.asFileTree.matching { include("*$EXT") }.files
                }
            }
        }
        generatedDefinitions.from(definitions)
        dependsOn(linkTasks)

        outputFile.set(apiBuildDir.get().resolve(project.name + EXT))
    }
    configureCheckTasks(apiBuildDir, apiBuild, extension, targetConfig, commonApiDump, commonApiCheck)
}

@Suppress("LongParameterList")
private fun Project.configureCheckTasks(
    apiBuildDir: Provider<File>,
    apiBuild: TaskProvider<out Task>,
    extension: ApiValidationExtension?,
    targetConfig: TargetConfig,
    commonApiDump: TaskProvider<out Task>? = null,
    commonApiCheck: TaskProvider<out Task>? = null,
) {
    val projectName = project.name
    val apiCheckDir = targetConfig.apiDir.map {
        projectDir.resolve(it).also { r ->
            logger.debug("Configuring api for {} to {}", targetConfig.targetName ?: "jvm", r)
        }
    }
    val apiCheck = task<KotlinApiCompareTask>(targetConfig.apiTaskName("Check")) {
        isEnabled = apiCheckEnabled(projectName, extension) && apiBuild.map { it.enabled }.getOrElse(true)
        group = "verification"
        description = "Checks signatures of public API against the golden value in API folder for $projectName"
        compareApiDumps(apiReferenceDir = apiCheckDir.get(), apiBuildDir = apiBuildDir.get())
        dependsOn(apiBuild)
    }

    val apiDump = task<Sync>(targetConfig.apiTaskName("Dump")) {
        isEnabled = apiCheckEnabled(projectName, extension) && apiBuild.map { it.enabled }.getOrElse(true)
        group = "other"
        description = "Syncs API from build dir to ${targetConfig.apiDir} dir for $projectName"
        from(apiBuildDir)
        into(apiCheckDir)
        dependsOn(apiBuild)
    }

    commonApiDump?.configure { dependsOn(apiDump) }

    when (commonApiCheck) {
        null -> project.tasks.named("check").configure { dependsOn(apiCheck) }
        else -> commonApiCheck.configure { dependsOn(apiCheck) }
    }
}
