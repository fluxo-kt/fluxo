import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.internal.lint.AndroidLintTask
import impl.MergeDetektBaselinesTask
import impl.checkIsRootProject
import impl.isDetektTaskAllowed
import impl.library
import impl.libsCatalog
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 *
 * @param ignoredBuildTypes List of Android build types for which to create no detekt task
 * @param ignoredFlavors List of Android build flavors for which to create no detekt task
 */
fun Project.setupVerification(
    ignoredBuildTypes: List<String> = listOf(),
    ignoredFlavors: List<String> = listOf(),
) {
    checkIsRootProject()

    val mergeLint = tasks.register(MERGE_LINT_TASK_NAME, ReportMergeTask::class.java) {
        group = JavaBasePlugin.VERIFICATION_GROUP
        description = "Merges all Lint reports from all modules to the root one"
        output.set(project.layout.buildDirectory.file("lint-merged.sarif"))
    }
    val mergeDetekt = tasks.register(MERGE_DETEKT_TASK_NAME, ReportMergeTask::class.java) {
        group = JavaBasePlugin.VERIFICATION_GROUP
        description = "Merges all Detekt reports from all modules to the root one"
        output.set(project.layout.buildDirectory.file("detekt-merged.sarif"))
    }

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(mergeDetekt, mergeLint)
    }

    val detektCompose = rootProject.file("detekt-compose.yml")
    val hasDetektCompose = detektCompose.exists()
    val rootBasePath = rootProject.projectDir.absolutePath

    allprojects {
        plugins.apply(DETEKT_PLUGIN_ID)

        val detektBaselineFile = file("detekt-baseline.xml")
        val taskNames = gradle.startParameter.taskNames
        val mergeDetektBaselinesTask = when {
            !taskNames.any { it == MergeDetektBaselinesTask.TASK_NAME } -> null
            else -> tasks.register(
                MergeDetektBaselinesTask.TASK_NAME,
                MergeDetektBaselinesTask::class.java,
            ) {
                outputFile.set(detektBaselineFile)
            }
        }
        val detektMergeStarted = mergeDetektBaselinesTask != null
        val testStarted = taskNames.any { name ->
            arrayOf("check", "test").any { name.startsWith(it) }
        }

        val detektBaselineIntermediate = "$buildDir/intermediates/detekt/baseline"
        extensions.configure<DetektExtension> {
            val isCI by isCI()

            parallel = true
            buildUponDefaultConfig = true
            ignoreFailures = true
            autoCorrect = !isCI && !testStarted && !detektMergeStarted
            basePath = rootBasePath

            this.ignoredBuildTypes = ignoredBuildTypes
            this.ignoredFlavors = ignoredFlavors

            val files = arrayOf(
                file("detekt.yml"),
                rootProject.file("detekt.yml"),
                detektCompose,
            ).filter { it.exists() && it.canRead() }.toTypedArray()
            if (files.isNotEmpty()) {
                @Suppress("SpreadOperator")
                config.from(*files)
            }

            baseline = if (detektMergeStarted) {
                file("$detektBaselineIntermediate.xml")
            } else {
                file(detektBaselineFile)
            }
        }

        if (mergeDetektBaselinesTask != null) {
            val baselineTasks = tasks.withType<DetektCreateBaselineTask> {
                baseline.set(file("$detektBaselineIntermediate-$name.xml"))
            }
            mergeDetektBaselinesTask.configure {
                dependsOn(baselineTasks)
                baselineFiles.from(baselineTasks.map { it.baseline })
            }
        }

        val disableTests by disableTests()
        val detektTasks = tasks.withType<Detekt> {
            if (disableTests || !isDetektTaskAllowed()) {
                enabled = false
            }
            reports {
                sarif.required.set(true)
                html.required.set(true)
                txt.required.set(false)
                xml.required.set(false)
            }
        }
        val detektAll = tasks.register("detektAll") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Calls all available Detekt tasks for this project"
            dependsOn(detektTasks)
        }
        tasks.matching { it.name == "check" }.configureEach {
            dependsOn(detektAll)
        }
        mergeDetekt.configure {
            dependsOn(detektTasks)
            input.from(detektTasks.map { it.sarifReportFile })
        }

        dependencies {
            val conf = "detektPlugins"
            val libsCatalog = rootProject.libsCatalog

            add(conf, libsCatalog.library("detekt-formatting"))
            if (hasDetektCompose) {
                // Add to all modules (even ones that don't use Compose) as detecting Compose can be messy.
                add(conf, libsCatalog.library("detekt-compose"))
            }
        }


        plugins.withId("com.android.library") {
            setupLint(mergeLint)
        }
        plugins.withId("com.android.application") {
            setupLint(mergeLint)
        }
    }

    setupTestsReport()
}

private fun Project.setupLint(mergeLint: TaskProvider<ReportMergeTask>?) {
    val disableLint = !isGenericCompilationEnabled || disableTests().get()
    extensions.configure<CommonExtension<*, *, *, *>>("android") {
        lint {
            sarifReport = !disableLint
            htmlReport = !disableLint
            textReport = !disableLint
            xmlReport = false

            baseline = file("lint-baseline.xml")

            abortOnError = false
            absolutePaths = false
            checkAllWarnings = !disableLint
            checkDependencies = false
            checkReleaseBuilds = !disableLint
            explainIssues = false
            warningsAsErrors = !disableLint
        }
    }

    if (disableLint || mergeLint == null) {
        return
    }
    tasks.withType<AndroidLintTask> {
        val lintTask = this
        if (name.startsWith("lintReport")) {
            mergeLint.configure {
                input.from(lintTask.sarifReportOutputFile)
                dependsOn(lintTask)
            }
        }
    }
}

private const val DETEKT_PLUGIN_ID = "io.gitlab.arturbosch.detekt"
private const val MERGE_LINT_TASK_NAME = "mergeLintSarif"
private const val MERGE_DETEKT_TASK_NAME = "mergeDetektSarif"
