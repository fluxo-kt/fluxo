import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.diagnostics.DependencyReportTask
import org.gradle.api.tasks.options.Option
import java.io.File

abstract class ReleaseDependenciesCreateFilesTask : DependencyReportTask() {
    internal companion object {
        internal const val DEPS_DIR = "dependencies"
    }

    @get:Input
    @get:Optional
    @set:Option(
        option = "directoryName",
        description = "The name of the directory where the dependency files are stored"
    )
    var directoryName: String? = DEPS_DIR

    @Suppress("UnstableApiUsage")
    override fun calculateReportModelFor(project: Project): DependencyReportModel {
        val isJavaLibrary = project.plugins.hasPlugin("java")

        val isAndroidLibrary =
            project.plugins.hasPlugin("com.android.application") ||
                project.plugins.hasPlugin("com.android.library")

        // We only handle android/java libraries. This plugin also picks up 'parent' modules that
        // don't have any code or build.gradle files.
        if (isAndroidLibrary) {
            setConfiguration("releaseRuntimeClasspath")
        } else if (isJavaLibrary) {
            setConfiguration("runtimeClasspath")
        }
        return super.calculateReportModelFor(project)
    }

    override fun getOutputFile(): File? {
        val directoryName = checkNotNull(directoryName) { "directoryName was not supplied" }

        return project
            .rootProject
            .file("build/release-dependencies-diff/$directoryName/${project.fileName}")
    }

    private val Project.fileName: String
        get() = project.path.replace(":", "_")
}
