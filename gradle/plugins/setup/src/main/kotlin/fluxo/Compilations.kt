package fluxo

import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.targets
import org.jetbrains.kotlin.konan.target.Family

internal object Compilations {

    val isGenericEnabled: Boolean get() = isValidOs { it.isLinux }
    val isDarwinEnabled: Boolean get() = isValidOs { it.isMacOsX }
    val isWindowsEnabled: Boolean get() = isValidOs { it.isWindows }

    // Try to overcome the Gradle livelock issue
    // https://github.com/gradle/gradle/issues/20455#issuecomment-1327259045
    fun isGenericEnabledForProject(project: Project): Boolean = when {
        project.isCI().get() -> isDarwinEnabled
        else -> isGenericEnabled
    }

    private fun isValidOs(predicate: (OperatingSystem) -> Boolean): Boolean =
        !EnvParams.splitTargets || predicate(OperatingSystem.current())
}

internal val Project.isGenericCompilationEnabled
    inline get() = Compilations.isGenericEnabledForProject(this)

internal fun KotlinProjectExtension.disableCompilationsOfNeeded() {
    targets.forEach {
        it.disableCompilationsOfNeeded()
    }
}

private fun KotlinTarget.disableCompilationsOfNeeded() {
    if (!isCompilationAllowed()) {
        println("$project, $this, compilation disabled")
        disableCompilations()
    }
}

private fun KotlinTarget.disableCompilations() {
    compilations.configureEach {
        compileTaskProvider.get().enabled = false
    }
}

private fun KotlinTarget.isCompilationAllowed(): Boolean =
    when (platformType) {
        KotlinPlatformType.common -> true

        KotlinPlatformType.jvm,
        KotlinPlatformType.js,
        KotlinPlatformType.androidJvm,
        KotlinPlatformType.wasm,
        -> project.isGenericCompilationEnabled

        KotlinPlatformType.native -> (this as KotlinNativeTarget).konanTarget.family.isCompilationAllowed(project)
    }

private fun Family.isCompilationAllowed(project: Project): Boolean =
    when (this) {
        Family.OSX,
        Family.IOS,
        Family.TVOS,
        Family.WATCHOS,
        -> Compilations.isDarwinEnabled

        Family.LINUX -> Compilations.isGenericEnabled

        Family.ANDROID,
        Family.WASM,
        -> project.isGenericCompilationEnabled

        Family.MINGW -> Compilations.isWindowsEnabled

        Family.ZEPHYR -> error("Unsupported family: $this")
    }
