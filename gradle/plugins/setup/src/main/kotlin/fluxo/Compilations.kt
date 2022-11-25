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
    fun isGenericEnabledLivelockAware(project: Project): Boolean = when {
        project.isCI().get() -> isDarwinEnabled
        else -> isGenericEnabled
    }

    private fun isValidOs(predicate: (OperatingSystem) -> Boolean): Boolean =
        !EnvParams.splitTargets || predicate(OperatingSystem.current())
}

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
        compileKotlinTask.enabled = false
    }
}

private fun KotlinTarget.isCompilationAllowed(): Boolean =
    when (platformType) {
        KotlinPlatformType.common -> true

        KotlinPlatformType.jvm,
        KotlinPlatformType.js,
        KotlinPlatformType.androidJvm,
        KotlinPlatformType.wasm -> Compilations.isGenericEnabledLivelockAware(project)

        KotlinPlatformType.native -> (this as KotlinNativeTarget).isCompilationAllowed()
    }

private fun KotlinNativeTarget.isCompilationAllowed(): Boolean =
    when (val family = konanTarget.family) {
        Family.OSX,
        Family.IOS,
        Family.TVOS,
        Family.WATCHOS -> Compilations.isDarwinEnabled

        Family.LINUX,
        Family.ANDROID,
        Family.WASM -> Compilations.isGenericEnabled

        Family.MINGW -> Compilations.isWindowsEnabled
        Family.ZEPHYR -> error("Unsupported family: $family")
    }
