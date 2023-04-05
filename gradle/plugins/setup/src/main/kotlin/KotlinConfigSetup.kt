import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions

@Suppress("LongParameterList")
class KotlinConfigSetup(
    /**
     * Override for the Kotlin lang version used.
     * Use "latest" for the latest value.
     */
    val kotlinLangVersion: String? = null,

    /**
     * Override for the Java lang target version used.
     */
    val javaLangTarget: String? = null,

    /**
     * Don't set up JVM toolchain by default as it can slow down the build.
     */
    val setupJvmToolchain: Boolean = false,

    /**
     * Add basic coroutines dependencies and opt-ins
     */
    val setupCoroutines: Boolean = true,

    /**
     * Set up basic KotlinX serialization
     */
    val setupSerialization: Boolean = false,

    /**
     * Generate metadata for Java 1.8+ reflection on method parameters
     */
    val javaParameters: Boolean = false,

    /**
     * `class` mode provides lambdas arguments names
     */
    val alwaysIndyLambdas: Boolean = true,

    /**
     * Using the new faster version of JAR FS should make build faster,
     * but it is experimental and causes warning (auto turned off when [warningsAsErrors]).
     */
    val useExperimentalFastJarFs: Boolean = true,

    /**
     * Remove utility bytecode, eliminating names/data leaks in release obfuscated code.
     */
    val removeAssertionsInRelease: Boolean = true,

    /**
     * Opt-in for internal Kotlin features
     */
    val optInInternal: Boolean = false,

    /**
     * Report an error for any Kotlin warnings
     */
    val warningsAsErrors: Boolean = false,

    /**
     * Turn on `suppressKotlinVersionCompatibilityCheck` for Jetpack Compose
     */
    val suppressKotlinComposeCompatCheck: Boolean = false,

    /**
     * In the progressive mode, deprecations, and bug fixes for unstable code take effect immediately,
     * instead of going through a graceful migration cycle.
     * The code written in the progressive mode is backwards compatible.
     * However, written in a non-progressive mode may cause compilation errors in the progressive mode.
     *
     * Only applied for the latest [kotlinLangVersion] (otherwise meaningless).
     */
    val progressive: Boolean = true,

    /**
     * Don't use `compileOnly` dependencies in KMP setup
     */
    val noMultiplatformCompileOnly: Boolean = false,

    /**
     * Use it only for an actual app, not libraries.
     *
     * See: https://docs.gradle.org/current/userguide/java_platform_plugin.html
     */
    val allowGradlePlatform: Boolean = false,

    /**
     * Kotlin opt-ins to add
     */
    val optIns: List<String> = emptyList(),

    val configurator: (KotlinCommonCompilerOptions.() -> Unit)? = null,
)
