import fluxo.library
import fluxo.libsCatalog
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class DetektPlugin : Plugin<Project> {
    internal companion object {
        internal const val PLUGIN_ID = "io.gitlab.arturbosch.detekt"
    }

    override fun apply(target: Project) {
        target.plugins.apply(PLUGIN_ID)
        target.plugins.withId(PLUGIN_ID) {
            val rootProject = target.rootProject

            target.extensions.configure<DetektExtension> {
                parallel = true
                buildUponDefaultConfig = true
                baseline = target.file("detekt-baseline.xml")
                basePath = rootProject.projectDir.absolutePath

                val files = arrayOf(
                    target.file("detekt.yml"),
                    target.rootProject.file("detekt.yml"),
                    target.rootProject.file("detekt-compose.yml"),
                ).filter { it.exists() }.toTypedArray()
                if (files.isNotEmpty()) {
                    @Suppress("SpreadOperator")
                    config.from(*files)
                }
            }

            val detektTask = target.tasks.named("detekt", Detekt::class.java)
            detektTask.configure {
                reports.sarif.required.set(true)
            }

            rootProject.plugins.withId("fluxo-collect-sarif") {
                rootProject.tasks.named(CollectSarifPlugin.MERGE_DETEKT_TASK_NAME, ReportMergeTask::class.java) {
                    input.from(detektTask.map { it.sarifReportFile }.orNull)
                    mustRunAfter(detektTask)
                }
            }
        }

        target.dependencies {
            val libsCatalog = target.rootProject.libsCatalog
            add("detektPlugins", libsCatalog.library("detekt-formatting"))
            // Add to all modules (even ones that don't use Compose) as detecting Compose can be messy.
            add("detektPlugins", libsCatalog.library("detekt-compose"))
        }
    }
}
