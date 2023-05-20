package js.api

import impl.isTestRelated
import kotlinx.validation.KotlinApiBuildTask
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.withType
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import java.io.File

/**
 *
 * @see kotlinx.validation.KotlinApiBuildTask
 * @see kotlinx.validation.configureApiTasks
 * @see kotlinx.validation.BinaryCompatibilityValidatorPlugin
 *
 * @see org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
 * @see org.jetbrains.kotlin.gradle.targets.js.ir.SyncExecutableTask
 * @see org.jetbrains.kotlin.gradle.targets.js.typescript.TypeScriptValidationTask
 */
@CacheableTask
internal abstract class KotlinJsApiBuildTask : DefaultTask() {

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generatedDefinitions: ConfigurableFileCollection

    @get:OutputFile
    var outputFile: File = project.buildDir.resolve(API_PATH).resolve(project.name + EXT)

    init {
        val projectName = project.name
        enabled = apiCheckEnabled(projectName, project.apiValidationExtensionOrNull)
        group = JavaBasePlugin.VERIFICATION_GROUP
        description = "Collects built Kotlin TS definitions as API for 'js' compilations of $projectName. " +
                "Complementary task and shouldn't be called manually"
    }

    @TaskAction
    fun generate() {
        val files = generatedDefinitions.files
        if (files.isEmpty()) {
            return
        }
        if (files.size > 1) {
            logger.warn("Ambigous generated definitions, taking only first: $generatedDefinitions")
        }
        files.first().copyTo(outputFile)
    }

    internal companion object {
        private const val API_PATH = "api/js"
        private const val NAME = "apiJsBuild"
        private const val EXT = ".d.ts"

        internal fun setupTask(
            project: Project,
            extension: KotlinMultiplatformExtension,
        ): TaskProvider<KotlinJsApiBuildTask>? {
            // https://scans.gradle.com/s/5slylgpxgulk2/timeline

            if (!apiCheckEnabled(project.name, project.apiValidationExtensionOrNull)) {
                return null
            }

            val apiJsBuild = project.tasks.register(NAME, KotlinJsApiBuildTask::class.java)

            project.afterEvaluate {
                val compilations = extension.targets.asSequence()
                    .filterIsInstance<KotlinJsIrTarget>()
                    .flatMap { it.compilations }
                    .filter { !it.isTestRelated() }

                var noCompilations = true
                var noBinaries = true

                @Suppress("INVISIBLE_MEMBER")
                val binaries = compilations.flatMap { noCompilations = false; it.binaries }
                    .filterIsInstance<JsIrBinary>()
                    .filter { it.generateTs && it.mode == KotlinJsBinaryMode.PRODUCTION }

                for (binary in binaries) {
                    noBinaries = false
                    val linkTask = binary.linkTask
                    apiJsBuild.configure {
                        val files = linkTask.flatMap {
                            provider {
                                it.destinationDirectory.asFileTree.matching { include("*$EXT") }.files
                            }
                        }
                        generatedDefinitions.from(files)
                        dependsOn(linkTask)
                    }
                }

                if (noBinaries) {
                    apiJsBuild.configure { enabled = false }
                    if (!noCompilations) {
                        logger.warn(
                            "Please, enable TS definitions with `generateTypeScriptDefinitions()` for :${project.name}. " +
                                    "No production binaries found with TS definitions enabled. " +
                                    "Kotlin JS API verification is not possible."
                        )
                    }
                } else {
                    project.tasks.withType<KotlinApiBuildTask> {
                        finalizedBy(apiJsBuild)
                    }
                }
            }

            return apiJsBuild
        }
    }
}
