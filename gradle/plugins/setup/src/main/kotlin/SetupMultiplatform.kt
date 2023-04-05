@file:Suppress("TooManyFunctions")

import com.android.build.gradle.LibraryExtension
import impl.implementation
import impl.libsCatalog
import impl.onLibrary
import impl.optionalVersion
import impl.testImplementation
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

typealias MultiplatformConfigurator = KotlinMultiplatformExtension.() -> Unit

internal val Project.multiplatformExtension: KotlinMultiplatformExtension
    get() = kotlinExtension as KotlinMultiplatformExtension

// FIXME: Improve setup, completely remove disabled sources/plugins/tasks. Add everything dynamically, see:
//  https://github.com/05nelsonm/gradle-kmp-configuration-plugin

@Suppress("LongParameterList")
fun Project.setupMultiplatform(
    config: KotlinConfigSetup = requireDefaults(),
    namespace: String? = null,
    setupCompose: Boolean = false,
    enableBuildConfig: Boolean? = null,
    optIns: List<String> = emptyList(),
    configurator: MultiplatformConfigurator? = getDefaults(),
    configureAndroid: (LibraryExtension.() -> Unit)? = null,
    body: MultiplatformConfigurator? = null,
) {
    multiplatformExtension.apply {
        logger.lifecycle("> Conf :setupMultiplatform")

        setupKotlinExtension(this, config, optIns)
        setupMultiplatformDependencies(config, project)

        configurator?.invoke(this)

        setupSourceSets(project)

        val jbCompose = (this as ExtensionAware).extensions.findByName("compose")
        val setupAndroidCompose = setupCompose && jbCompose == null
        if (isMultiplatformTargetEnabled(Target.ANDROID) || configureAndroid != null) {
            setupAndroidCommon(
                namespace = checkNotNull(namespace) { "namespace is required for android setup" },
                setupKsp = false,
                setupRoom = false,
                setupCompose = setupAndroidCompose,
                enableBuildConfig = enableBuildConfig,
                kotlinConfig = config,
            )
            project.extensions.configure<LibraryExtension>("android") {
                configureAndroid?.invoke(this)
            }
        }

        if (setupCompose) {
            setupCompose(project, jbCompose, config)
        }

        body?.invoke(this)

        disableCompilationsOfNeeded(project)
    }
}

private fun KotlinMultiplatformExtension.setupMultiplatformDependencies(config: KotlinConfigSetup, project: Project) {
    setupSourceSets {
        val libs = project.libsCatalog

        val kotlinVersion = libs.optionalVersion("kotlin")
        project.dependencies.apply {
            val kotlinBom = enforcedPlatform(kotlin("bom", kotlinVersion))
            when {
                config.allowGradlePlatform -> implementation(kotlinBom)
                else -> testImplementation(kotlinBom)
            }

            if (config.setupCoroutines) {
                libs.onLibrary("kotlinx-coroutines-bom") {
                    when {
                        config.allowGradlePlatform -> implementation(enforcedPlatform(it))
                        else -> testImplementation(enforcedPlatform(it))
                    }
                }
            }

            libs.onLibrary("square-okio-bom") { implementation(platform(it)) }
            libs.onLibrary("square-okhttp-bom") { implementation(platform(it)) }
        }

        common.main.dependencies {
            implementation(kotlin("stdlib", kotlinVersion), excludeAnnotations)

            if (config.setupCoroutines) {
                libs.onLibrary("kotlinx-coroutines-core") { implementation(it) }
            }
        }

        common.test.dependencies {
            implementation(kotlin("reflect"))
            implementation(kotlin("test"))

            libs.onLibrary("kotlinx-datetime") { implementation(it) }

            if (config.setupCoroutines) {
                libs.onLibrary("kotlinx-coroutines-test") { implementation(it) }
                libs.onLibrary("test-turbine") { implementation(it) }
            }
        }
    }
}

private fun KotlinMultiplatformExtension.setupSourceSets(project: Project) {
    setupSourceSets {
        setupCommonJavaSourceSets(project, javaSet)

        val nativeSet = nativeSet.toMutableSet()
        val jsSet = jsSet
        if (nativeSet.isNotEmpty() || jsSet.isNotEmpty()) {
            val nativeAndJs by bundle()
            nativeAndJs dependsOn common

            val native by bundle()
            native dependsOn nativeAndJs

            if (jsSet.isNotEmpty()) {
                val js by bundle()
                js dependsOn nativeAndJs
                native dependsOn common
            }

            if (nativeSet.isNotEmpty()) {
                darwinSet.ifNotEmpty {
                    nativeSet -= this
                    val apple by bundle()
                    apple dependsOn native
                    this dependsOn apple
                }

                linuxSet.ifNotEmpty {
                    nativeSet -= this
                    val linux by bundle()
                    linux dependsOn native
                    this dependsOn linux
                }

                mingwSet.ifNotEmpty {
                    nativeSet -= this
                    val mingw by bundle()
                    mingw dependsOn native
                    this dependsOn mingw
                }

                nativeSet dependsOn native
            }
        }
    }
}

private fun MultiplatformSourceSets.setupCommonJavaSourceSets(project: Project, sourceSet: Set<SourceSetBundle>) {
    if (sourceSet.isEmpty()) {
        return
    }

    val java by bundle()
    java dependsOn common
    sourceSet dependsOn java

    val libs = project.libsCatalog
    val constraints = project.dependencies.constraints
    (sourceSet + java).main.dependencies {
        val compileOnlyWithConstraint: (Any) -> Unit = {
            compileOnly(it)
            constraints.implementation(it)
        }

        // Java-only annotations
        libs.onLibrary("jetbrains-annotation", compileOnlyWithConstraint)
        compileOnlyWithConstraint(JSR305_DEPENDENCY)

        // TODO: Use `compileOnlyApi` for transitively included compile-only dependencies.
        // https://issuetracker.google.com/issues/216293107
        // https://issuetracker.google.com/issues/216305675
        //
        // Also note that atm androidx annotations aren't usable in the common source sets!
        // https://issuetracker.google.com/issues/273468771
        libs.onLibrary("androidx-annotation") { compileOnlyWithConstraint(it) }
    }
}

private fun KotlinMultiplatformExtension.setupCompose(project: Project, jbCompose: Any?, config: KotlinConfigSetup) {
    setupSourceSets {
        val libs = project.libsCatalog

        // AndroidX Compose
        if (jbCompose == null) {
            val constraints = project.dependencies.constraints
            androidSet.main.dependencies {
                // Support compose @Stable and @Immutable annotations
                libs.onLibrary("androidx-compose-runtime") {
                    compileOnly(it)
                    constraints.implementation(it)
                }
            }
        }

        // Jetbrains KMP Compose
        else {
            val composeDependency = jbCompose as org.jetbrains.compose.ComposePlugin.Dependencies
            val composeRuntime = composeDependency.runtime

            common.main.dependencies {
                // Support compose @Stable and @Immutable annotations
                if (config.noMultiplatformCompileOnly) {
                    // A compileOnly dependencies aren't applicable for Kotlin/Native.
                    // Use 'implementation' or 'api' dependency type instead.
                    implementation(composeRuntime)
                } else {
                    compileOnly(composeRuntime)
                }
            }
        }
    }
}


fun KotlinMultiplatformExtension.setupSourceSets(block: MultiplatformSourceSets.() -> Unit) {
    MultiplatformSourceSets(targets, sourceSets).block()
}

internal enum class Target {
    ANDROID,
    JVM,
}

internal fun Project.isMultiplatformTargetEnabled(target: Target): Boolean =
    multiplatformExtension.isMultiplatformTargetEnabled(target)

internal fun KotlinTargetsContainer.isMultiplatformTargetEnabled(target: Target): Boolean =
    targets.any {
        when (it.platformType) {
            KotlinPlatformType.androidJvm -> target == Target.ANDROID
            KotlinPlatformType.jvm -> target == Target.JVM
            KotlinPlatformType.common,
            KotlinPlatformType.js,
            KotlinPlatformType.native,
            KotlinPlatformType.wasm,
            -> false
        }
    }

class MultiplatformSourceSets
internal constructor(
    private val targets: NamedDomainObjectCollection<KotlinTarget>,
    private val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
) : NamedDomainObjectContainer<KotlinSourceSet> by sourceSets {

    val common: SourceSetBundle by bundle()

    /** All enabled targets */
    val allSet: Set<SourceSetBundle> = targets.toSourceSetBundles()

    /** androidJvm, jvm */
    val javaSet: Set<SourceSetBundle> = targets
        .filter { it.platformType in setOf(KotlinPlatformType.androidJvm, KotlinPlatformType.jvm) }
        .toSourceSetBundles()

    /** androidJvm */
    val androidSet: Set<SourceSetBundle> = targets
        .filter { it.platformType == KotlinPlatformType.androidJvm }
        .toSourceSetBundles()

    /** js */
    val jsSet: Set<SourceSetBundle> = targets
        .filter { it.platformType == KotlinPlatformType.js }
        .toSourceSetBundles()

    /** All Kotlin/Native targets */
    val nativeSet: Set<SourceSetBundle> = nativeSourceSets()
    val linuxSet: Set<SourceSetBundle> = nativeSourceSets(Family.LINUX)
    val mingwSet: Set<SourceSetBundle> = nativeSourceSets(Family.MINGW)
    val androidNativeSet: Set<SourceSetBundle> = nativeSourceSets(Family.ANDROID)
    val wasmSet: Set<SourceSetBundle> = nativeSourceSets(Family.WASM)

    /** All Darwin targets */
    val darwinSet: Set<SourceSetBundle> =
        nativeSourceSets(Family.IOS, Family.OSX, Family.WATCHOS, Family.TVOS)
    val iosSet: Set<SourceSetBundle> = nativeSourceSets(Family.IOS)
    val watchosSet: Set<SourceSetBundle> = nativeSourceSets(Family.WATCHOS)
    val tvosSet: Set<SourceSetBundle> = nativeSourceSets(Family.TVOS)
    val macosSet: Set<SourceSetBundle> = nativeSourceSets(Family.OSX)

    private fun nativeSourceSets(vararg families: Family = Family.values()): Set<SourceSetBundle> =
        targets.filterIsInstance<KotlinNativeTarget>()
            .filter { it.konanTarget.family in families }
            .toSourceSetBundles()

    private fun Iterable<KotlinTarget>.toSourceSetBundles(): Set<SourceSetBundle> {
        return filter { it.platformType != KotlinPlatformType.common }
            .map { it.getSourceSetBundle() }
            .toSet()
    }

    private fun KotlinTarget.getSourceSetBundle(): SourceSetBundle = if (compilations.isEmpty()) {
        bundle(name)
    } else {
        SourceSetBundle(
            main = compilations.getByName("main").defaultSourceSet,
            test = compilations.getByName("test").defaultSourceSet,
        )
    }
}

fun NamedDomainObjectContainer<out KotlinSourceSet>.bundle(name: String): SourceSetBundle {
    return SourceSetBundle(
        main = maybeCreate("${name}Main"),
        // Support for androidSourceSetLayout v2
        // https://kotlinlang.org/docs/whatsnew18.html#kotlinsourceset-naming-schema
        test = maybeCreate(if (name == "android") "${name}UnitTest" else "${name}Test"),
    )
}

fun NamedDomainObjectContainer<out KotlinSourceSet>.bundle(): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, SourceSetBundle>> =
    PropertyDelegateProvider { _, property ->
        val bundle = bundle(property.name)
        ReadOnlyProperty { _, _ -> bundle }
    }

data class SourceSetBundle(
    val main: KotlinSourceSet,
    val test: KotlinSourceSet,
)


// region Dependecies declaration

operator fun SourceSetBundle.plus(other: SourceSetBundle): Set<SourceSetBundle> =
    this + setOf(other)

operator fun SourceSetBundle.plus(other: Set<SourceSetBundle>): Set<SourceSetBundle> =
    setOf(this) + other

infix fun SourceSetBundle.dependsOn(other: SourceSetBundle) {
    main.dependsOn(other.main)
    test.dependsOn(other.test)
}

infix fun Iterable<SourceSetBundle>.dependsOn(other: Iterable<SourceSetBundle>) {
    forEach { left ->
        other.forEach { right ->
            left.dependsOn(right)
        }
    }
}

infix fun SourceSetBundle.dependsOn(other: Iterable<SourceSetBundle>) {
    listOf(this) dependsOn other
}

infix fun Iterable<SourceSetBundle>.dependsOn(other: SourceSetBundle) {
    this dependsOn listOf(other)
}


infix fun KotlinSourceSet.dependsOn(other: SourceSetBundle) {
    dependsOn(if ("Test" in name) other.test else other.main)
}

@JvmName("dependsOnBundles")
infix fun KotlinSourceSet.dependsOn(other: Iterable<SourceSetBundle>) {
    other.forEach { right ->
        dependsOn(right)
    }
}

infix fun KotlinSourceSet.dependsOn(other: Iterable<KotlinSourceSet>) {
    other.forEach { right ->
        dependsOn(right)
    }
}


val Iterable<SourceSetBundle>.main: List<KotlinSourceSet> get() = map { it.main }

val Iterable<SourceSetBundle>.test: List<KotlinSourceSet> get() = map { it.test }

infix fun Iterable<KotlinSourceSet>.dependencies(configure: KotlinDependencyHandler.() -> Unit) {
    forEach { left ->
        left.dependencies(configure)
    }
}


fun KotlinDependencyHandler.ksp(dependencyNotation: Any): Dependency? {
    // Starting from KSP 1.0.1, applying KSP on a multiplatform project requires
    // instead of writing the ksp("dep")
    // use ksp<Target>() or add(ksp<SourceSet>).
    // https://kotlinlang.org/docs/ksp-multiplatform.html
    val parent = (this as DefaultKotlinDependencyHandler).parent
    var configurationName = parent.compileOnlyConfigurationName.replace("compileOnly", "", ignoreCase = true)
    if (configurationName.startsWith("commonMain", ignoreCase = true)) {
        configurationName += "Metadata"
    } else {
        configurationName = configurationName.replace("Main", "", ignoreCase = true)
    }
    configurationName = "ksp${configurationName[0].uppercase()}${configurationName.substring(1)}"
    project.logger.lifecycle(">>> ksp configurationName: $configurationName")
    return project.dependencies.add(configurationName, dependencyNotation)
}

// endregion


// region Darwin compat

fun KotlinMultiplatformExtension.iosCompat(
    x64: String? = DEFAULT_TARGET_NAME,
    arm64: String? = DEFAULT_TARGET_NAME,
    simulatorArm64: String? = DEFAULT_TARGET_NAME,
) {
    enableTarget(name = x64, enableDefault = { iosX64() }, enableNamed = { iosX64(it) })
    enableTarget(name = arm64, enableDefault = { iosArm64() }, enableNamed = { iosArm64(it) })
    enableTarget(
        name = simulatorArm64,
        enableDefault = { iosSimulatorArm64() },
        enableNamed = { iosSimulatorArm64(it) },
    )
}

fun KotlinMultiplatformExtension.watchosCompat(
    x64: String? = DEFAULT_TARGET_NAME,
    arm32: String? = DEFAULT_TARGET_NAME,
    arm64: String? = DEFAULT_TARGET_NAME,
    simulatorArm64: String? = DEFAULT_TARGET_NAME,
) {
    enableTarget(name = x64, enableDefault = { watchosX64() }, enableNamed = { watchosX64(it) })
    enableTarget(name = arm32, enableDefault = { watchosArm32() }, enableNamed = { watchosArm32(it) })
    enableTarget(name = arm64, enableDefault = { watchosArm64() }, enableNamed = { watchosArm64(it) })
    enableTarget(
        name = simulatorArm64,
        enableDefault = { watchosSimulatorArm64() },
        enableNamed = { watchosSimulatorArm64(it) },
    )
}

fun KotlinMultiplatformExtension.tvosCompat(
    x64: String? = DEFAULT_TARGET_NAME,
    arm64: String? = DEFAULT_TARGET_NAME,
    simulatorArm64: String? = DEFAULT_TARGET_NAME,
) {
    enableTarget(name = x64, enableDefault = { tvosX64() }, enableNamed = { tvosX64(it) })
    enableTarget(name = arm64, enableDefault = { tvosArm64() }, enableNamed = { tvosArm64(it) })
    enableTarget(
        name = simulatorArm64,
        enableDefault = { tvosSimulatorArm64() },
        enableNamed = { tvosSimulatorArm64(it) },
    )
}

fun KotlinMultiplatformExtension.macosCompat(x64: String? = DEFAULT_TARGET_NAME, arm64: String? = DEFAULT_TARGET_NAME) {
    enableTarget(name = x64, enableDefault = { macosX64() }, enableNamed = { macosX64(it) })
    enableTarget(name = arm64, enableDefault = { macosArm64() }, enableNamed = { macosArm64(it) })
}

private fun KotlinMultiplatformExtension.enableTarget(
    name: String?,
    enableDefault: KotlinMultiplatformExtension.() -> Unit,
    enableNamed: KotlinMultiplatformExtension.(String) -> Unit,
) {
    if (name != null) {
        if (name == DEFAULT_TARGET_NAME) {
            enableDefault()
        } else {
            enableNamed(name)
        }
    }
}

private const val DEFAULT_TARGET_NAME = "fluxo.DEFAULT_TARGET_NAME"

// endregion
