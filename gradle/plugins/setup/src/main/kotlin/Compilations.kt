import impl.EnvParams
import impl.isTaskAllowedBasedByName
import impl.isTestRelated
import impl.splitCamelCase
import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.targets
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.konan.target.Family

// TODO: JS is still initialized on MINGW64 when splitTargets enabled
//  https://github.com/fluxo-kt/fluxo/actions/runs/3831771108/jobs/6521297773

object Compilations {

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

val Project.isGenericCompilationEnabled
    get() = Compilations.isGenericEnabledForProject(this)

internal fun KotlinProjectExtension.disableCompilationsOfNeeded(project: Project) {
    val disableTests by project.disableTests()
    targets.forEach {
        it.disableCompilationsOfNeeded(disableTests)
    }

    if (!EnvParams.splitTargets) {
        if (disableTests) {
            // yarn.lock calculated differently without tests, ignore mismatch
            project.rootProject.plugins.withType(YarnPlugin::class.java) {
                with(project.rootProject.the<YarnRootExtension>()) {
                    yarnLockMismatchReport = YarnLockMismatchReport.NONE
                    yarnLockAutoReplace = false
                    reportNewYarnLock = false
                }
            }
        }
        return
    }
    project.afterEvaluate {
        project.tasks.withType<org.gradle.jvm.tasks.Jar> {
            if (enabled && !isTaskAllowedBasedByName()) {
                logger.info("{}, {}, jar disabled", project, this)
                enabled = false
            }
        }

        if (!project.isGenericCompilationEnabled) {
            /**
             * @see org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsSetupTask
             * @see org.jetbrains.kotlin.gradle.targets.js.npm.PublicPackageJsonTask
             * @see org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
             * @see org.jetbrains.kotlin.gradle.targets.js.typescript.TypeScriptValidationTask
             * @see org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockStoreTask
             */
            project.tasks
                .matching { it::class.java.name.startsWith("org.jetbrains.kotlin.gradle.targets.js.") }
                .configureEach {
                    if (enabled) {
                        logger.info("{}, {}, task disabled", project, this)
                        enabled = false
                    }
                }
        }
    }
}

private fun KotlinTarget.disableCompilationsOfNeeded(disableTests: Boolean) {
    val logger = project.logger
    if (!isCompilationAllowed()) {
        logger.info("{}, {}, target compilations disabled", project, this)
        disableCompilations(testOnly = false)
    } else if (disableTests) {
        logger.info("{}, {}, target test compilations disabled", project, this)
        disableCompilations(testOnly = true)
    }
}

private fun KotlinTarget.disableCompilations(testOnly: Boolean = false) {
    compilations.configureEach {
        if (!testOnly || isTestRelated()) {
            disableCompilation()
        }
    }
}

internal fun KotlinCompilation<*>.disableCompilation() {
    val task = compileTaskProvider.get()
    if (task.enabled) {
        task.enabled = false
        project.logger.lifecycle("task ':{}:{}' disabled, {}", project.name, task.name, this)
    }
}

private fun KotlinTarget.isCompilationAllowed(): Boolean = when (platformType) {
    KotlinPlatformType.common -> true

    KotlinPlatformType.jvm,
    KotlinPlatformType.js,
    KotlinPlatformType.androidJvm,
    KotlinPlatformType.wasm,
    -> project.isGenericCompilationEnabled

    KotlinPlatformType.native -> (this as KotlinNativeTarget).konanTarget.family.isCompilationAllowed(project)
}

private fun Family.isCompilationAllowed(project: Project): Boolean = when (this) {
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

internal fun AbstractTestTask.isTestTaskAllowed(): Boolean {
    return when (this) {
        is KotlinJsTest -> project.isGenericCompilationEnabled

        is KotlinNativeTest -> nativeFamilyFromString(platformFromTaskName(name)).isCompilationAllowed(project)

        // JVM/Android tests
        else -> project.isGenericCompilationEnabled
    }
}

private fun platformFromTaskName(name: String): String? = name.splitCamelCase(limit = 2).firstOrNull()

@Suppress("CyclomaticComplexMethod")
private fun nativeFamilyFromString(platform: String?): Family = when {
    platform.equals("watchos", ignoreCase = true) -> Family.WATCHOS
    platform.equals("tvos", ignoreCase = true) -> Family.TVOS
    platform.equals("ios", ignoreCase = true) -> Family.IOS

    platform.equals("darwin", ignoreCase = true) ||
            platform.equals("apple", ignoreCase = true) ||
            platform.equals("macos", ignoreCase = true)
    -> Family.OSX

    platform.equals("android", ignoreCase = true) -> Family.ANDROID
    platform.equals("linux", ignoreCase = true) -> Family.LINUX
    platform.equals("wasm", ignoreCase = true) -> Family.WASM

    platform.equals("mingw", ignoreCase = true) ||
            platform.equals("win", ignoreCase = true) ||
            platform.equals("windows", ignoreCase = true)
    -> Family.MINGW

    else -> throw IllegalArgumentException("Unsupported family: $platform")
}
