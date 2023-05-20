package js.api

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings

/**
 *
 * @TODO Replace with Copy task?
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
    abstract val outputFile: RegularFileProperty

    init {
        // 'group' is not specified deliberately, so it will be hidden from ./gradlew tasks.
        description = "Collects built Kotlin TS definitions as API for 'js' compilations of ${project.name}. " +
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
        files.first().copyTo(outputFile.asFile.get(), overwrite = true)
    }

    internal companion object {
        internal const val EXT = ".d.ts"
    }
}
