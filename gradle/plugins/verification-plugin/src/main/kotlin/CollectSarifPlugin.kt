import fluxo.checkIsRootProject
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin

class CollectSarifPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.checkIsRootProject()

        // Required ReportMergeTask is from Detekt, add it
        target.plugins.apply(DetektPlugin.PLUGIN_ID)

        target.tasks.register(MERGE_LINT_TASK_NAME, ReportMergeTask::class.java) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            output.set(project.layout.buildDirectory.file("lint-merged.sarif"))
        }
        target.tasks.register(MERGE_DETEKT_TASK_NAME, ReportMergeTask::class.java) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            output.set(project.layout.buildDirectory.file("detekt-merged.sarif"))
        }
    }

    internal companion object {
        internal const val MERGE_LINT_TASK_NAME: String = "mergeLintSarif"
        internal const val MERGE_DETEKT_TASK_NAME: String = "mergeDetektSarif"
    }
}
