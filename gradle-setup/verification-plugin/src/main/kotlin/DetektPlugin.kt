import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin


class DetektPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.plugins.apply("io.gitlab.arturbosch.detekt")
        target.plugins.withId("io.gitlab.arturbosch.detekt") {
            val rootProject = target.rootProject

            target.extensions.configure<DetektExtension> {
                parallel = true
                buildUponDefaultConfig = true
                baseline = target.file("detekt-baseline.xml")
                basePath = rootProject.projectDir.absolutePath

                val localDetektConfig = target.file("detekt.yml")
                val rootDetektConfig = target.rootProject.file("detekt.yml")
                if (localDetektConfig.exists()) {
                    config.from(localDetektConfig, rootDetektConfig)
                } else {
                    config.from(rootDetektConfig)
                }
            }

            target.tasks.register("detektAll") {
                group = LifecycleBasePlugin.VERIFICATION_GROUP
                dependsOn(target.tasks.withType<Detekt>())
            }
            target.tasks.named("build").configure {
                dependsOn("detektAll")
            }

            val detektTask = target.tasks.named("detekt", Detekt::class.java)
            detektTask.configure {
                reports.sarif.required.set(true)
            }

            rootProject.plugins.withId("fluxo-collect-sarif") {
                rootProject.tasks.named(
                    CollectSarifPlugin.MERGE_DETEKT_TASK_NAME,
                    ReportMergeTask::class.java,
                ) {
                    input.from(detektTask.map { it.sarifReportFile }.orNull)
                    mustRunAfter(detektTask)
                }
            }
        }
    }
}
