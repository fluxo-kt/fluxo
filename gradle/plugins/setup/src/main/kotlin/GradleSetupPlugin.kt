import impl.isRootProject
import impl.libsCatalog
import impl.onVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.diagnostics.DependencyReportTask
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

@Suppress("unused", "EmptyFunctionBlock", "ktPropBy")
class GradleSetupPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        if (!target.isRootProject) {
            return
        }

        val isCI by target.isCI()
        if (isCI) target.logger.lifecycle("> Conf CI mode is enabled!")

        val isRelease by target.isRelease()
        if (isRelease) target.logger.lifecycle("> Conf RELEASE mode is enabled!")

        val useKotlinDebug by target.useKotlinDebug()
        if (useKotlinDebug) target.logger.lifecycle("> Conf USE_KOTLIN_DEBUG mode is enabled!")

        val areComposeMetricsEnabled by target.areComposeMetricsEnabled()
        if (areComposeMetricsEnabled) target.logger.lifecycle("> Conf COMPOSE_METRICS mode is enabled!")

        val isR8Disabled by target.isR8Disabled()
        if (isR8Disabled) target.logger.lifecycle("> Conf DISABLE_R8 mode is enabled!")

        val disableTests by target.disableTests()
        if (disableTests) target.logger.lifecycle("> Conf DISABLE_TESTS mode is enabled!")

        val isMaxDebug by target.isMaxDebugEnabled()
        if (isMaxDebug) target.logger.lifecycle("> Conf MAX_DEBUG mode is enabled!")

        val isDesugaringEnabled by target.isDesugaringEnabled()
        if (isDesugaringEnabled) target.logger.lifecycle("> Conf DESUGARING mode is enabled!")

        target.logger.lifecycle("> Conf JRE version is ${System.getProperty("java.version")}")


        target.subprojects {
            // Convenience task to print full dependencies tree for any module
            // Use `buildEnvironment` task for the report about plugins
            // https://docs.gradle.org/current/userguide/viewing_debugging_dependencies.html
            tasks.register<DependencyReportTask>("allDeps")
        }

        target.tasks.register<Task>(name = "resolveDependencies") {
            group = "other"
            description = "Resolve and prefetch dependencies"
            doLast {
                target.allprojects.forEach { p ->
                    p.configurations.plus(p.buildscript.configurations)
                        .filter { it.isCanBeResolved }
                        .forEach {
                            try {
                                it.resolve()
                            } catch (_: Throwable) {
                            }
                        }
                }
            }
        }

        // Fix Kotlin/JS incompatibilities by pinning the dependencies versions.
        // Workaround for https://youtrack.jetbrains.com/issue/KT-52776
        // Also see https://github.com/rjaros/kvision/blob/d9044ab/build.gradle.kts#L28
        target.allprojects {
            afterEvaluate {
                target.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
                    val libs = target.libsCatalog
                    target.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
                        lockFileDirectory = project.rootDir.resolve(".kotlin-js-store")
                        libs.onVersion("js-uaParserJs") { resolution("ua-parser-js", it) }
                    }
                    target.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().apply {
                        libs.onVersion("js-karma") { versions.karma.version = it }
                        libs.onVersion("js-mocha") { versions.mocha.version = it }
                        libs.onVersion("js-webpack") { versions.webpack.version = it }
                        libs.onVersion("js-webpackCli") { versions.webpackCli.version = it }
                        libs.onVersion("js-webpackDevServer") { versions.webpackDevServer.version = it }
                    }
                }
            }
        }

        target.ensureUnreachableTasksDisabled()
    }
}
