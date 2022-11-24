package fluxo

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.internal.lint.AndroidLintTask
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin

fun Project.setupVerification() {
    checkIsRootProject()

    val mergeLint = tasks.register(MERGE_LINT_TASK_NAME, ReportMergeTask::class.java) {
        group = JavaBasePlugin.VERIFICATION_GROUP
        output.set(project.layout.buildDirectory.file("lint-merged.sarif"))
    }
    val mergeDetekt = tasks.register(MERGE_DETEKT_TASK_NAME, ReportMergeTask::class.java) {
        group = JavaBasePlugin.VERIFICATION_GROUP
        output.set(project.layout.buildDirectory.file("detekt-merged.sarif"))
    }

    val detektCompose = rootProject.file("detekt-compose.yml")
    val hasDetektCompose = detektCompose.exists()
    val rootBasePath = rootProject.projectDir.absolutePath

    allprojects {
        plugins.apply(DETEKT_PLUGIN_ID)

        extensions.configure<DetektExtension> {
            parallel = true
            buildUponDefaultConfig = true
            ignoreFailures = true
            baseline = file("detekt-baseline.xml")
            basePath = rootBasePath

            val files = arrayOf(
                file("detekt.yml"),
                rootProject.file("detekt.yml"),
                detektCompose,
            ).filter { it.exists() }.toTypedArray()
            if (files.isNotEmpty()) {
                @Suppress("SpreadOperator")
                config.from(*files)
            }
        }

        val detektTasks = tasks.withType<Detekt> {
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
        mergeDetekt.configure {
            input.from(detektTasks.map { it.sarifReportFile })
            mustRunAfter(detektTasks)
        }
        tasks.findByName("check")?.configure<Task> {
            dependsOn(mergeLint)
            val isCi by isCI()
            if (isCi) {
                dependsOn(mergeDetekt, detektAll)
            }
        }

        dependencies {
            val libsCatalog = rootProject.libsCatalog

            add("detektPlugins", libsCatalog.library("detekt-formatting"))
            if (hasDetektCompose) {
                // Add to all modules (even ones that don't use Compose) as detecting Compose can be messy.
                add("detektPlugins", libsCatalog.library("detekt-compose"))
            }
        }


        plugins.withId("com.android.library") {
            setupLint(mergeLint)
        }
        plugins.withId("com.android.application") {
            setupLint(mergeLint)
        }
    }
}

private fun Project.setupLint(mergeLint: TaskProvider<ReportMergeTask>) {
    extensions.configure<CommonExtension<*, *, *, *>>("android") {
        lint {
            sarifReport = true
            htmlReport = true
            textReport = false
            xmlReport = false

            baseline = file("lint-baseline.xml")
            // absolutePaths = true
            warningsAsErrors = true
            checkAllWarnings = true
            abortOnError = false
            checkDependencies = true
        }
    }
    mergeLint.configure {
        arrayOf("lintReportDebug", "lintReportRelease").forEach { taskName ->
            input.from(tasks.named(taskName, AndroidLintTask::class.java).flatMap { it.sarifReportOutputFile })
        }
    }
}

private const val DETEKT_PLUGIN_ID = "io.gitlab.arturbosch.detekt"
private const val MERGE_LINT_TASK_NAME: String = "mergeLintSarif"
private const val MERGE_DETEKT_TASK_NAME: String = "mergeDetektSarif"
