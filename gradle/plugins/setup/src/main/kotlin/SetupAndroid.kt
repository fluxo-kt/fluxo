@file:Suppress("UnstableApiUsage", "ktPropBy", "TooManyFunctions")

import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationBaseFlavor
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryBaseFlavor
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.gradle.internal.lint.AndroidLintTask
import com.android.build.gradle.internal.lint.AndroidLintTextOutputTask
import com.android.builder.model.BaseConfig
import com.google.devtools.ksp.gradle.KspExtension
import impl.androidTestImplementation
import impl.compileOnlyWithConstraint
import impl.debugCompileOnly
import impl.debugImplementation
import impl.implementation
import impl.ksp
import impl.libsCatalog
import impl.onBundle
import impl.onLibrary
import impl.onVersion
import impl.runtimeOnly
import impl.testImplementation
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import java.io.File
import java.util.Properties

@Suppress("LongParameterList")
fun Project.setupAndroidLibrary(
    namespace: String,
    enableBuildConfig: Boolean? = null,
    setupRoom: Boolean = false,
    setupKsp: Boolean = setupRoom || hasKsp,
    setupCompose: Boolean = false,
    config: AndroidConfigSetup = requireDefaults(),
    kotlinConfig: KotlinConfigSetup = requireDefaults(),
    body: (LibraryExtension.() -> Unit)? = null,
) {
    /** @see com.android.build.api.dsl.LibraryExtension */
    extensions.configure<LibraryExtension>("android") {
        logger.lifecycle("> Conf :setupAndroidLibrary")

        setupKotlin(config = kotlinConfig)
        setupAndroidCommon(
            namespace = namespace,
            config = config,
            enableBuildConfig = enableBuildConfig,
            setupRoom = setupRoom,
            setupKsp = setupKsp,
            setupCompose = setupCompose,
            kotlinConfig = kotlinConfig,
        )

        body?.invoke(this)
    }
}

@Suppress("LongParameterList")
fun Project.setupAndroidApp(
    applicationId: String,
    versionCode: Int,
    versionName: String,
    enableBuildConfig: Boolean? = null,
    setupRoom: Boolean = false,
    setupKsp: Boolean = setupRoom || hasKsp,
    setupCompose: Boolean = false,
    config: AndroidConfigSetup = requireDefaults(),
    kotlinConfig: KotlinConfigSetup = requireDefaults(),
    body: (BaseAppModuleExtension.() -> Unit)? = null,
) {
    extensions.configure<BaseAppModuleExtension>("android") {
        logger.lifecycle("> Conf :setupAndroidApp")
        namespace = applicationId

        setupKotlin(config = kotlinConfig)


        defaultConfig {
            this.applicationId = applicationId
            this.versionCode = versionCode
            this.versionName = versionName
        }

        setupSigning(appExtension = this)

        setupAndroidCommon(
            namespace = applicationId,
            config = config,
            enableBuildConfig = enableBuildConfig,
            setupRoom = setupRoom,
            setupKsp = setupKsp,
            setupCompose = setupCompose,
            kotlinConfig = kotlinConfig,
        )

        body?.invoke(this)
    }
}

@Suppress("UnstableApiUsage", "LongParameterList")
internal fun Project.setupAndroidCommon(
    namespace: String,
    config: AndroidConfigSetup = requireDefaults(),
    enableBuildConfig: Boolean? = null,
    setupRoom: Boolean = false,
    setupKsp: Boolean = setupRoom || hasKsp,
    setupCompose: Boolean = false,
    kotlinConfig: KotlinConfigSetup = requireDefaults(),
) {
    // https://developer.android.com/studio/build/extend-agp
    val androidComponents = extensions.getByType(AndroidComponentsExtension::class.java)
    androidComponents.finalizeDsl {
        it.onFinalizeDsl(project)
    }

    extensions.configure<CommonExtension<*, *, *, *>>("android") {
        val ns = namespace.replace('-', '.').lowercase()
        logger.lifecycle("> Conf Android namespace '$ns'")
        this.namespace = ns

        setupAndroidBasic(config, project, setupRoom = setupRoom, setupKsp = setupKsp)

        // Java compatibility settings
        val libs = libsCatalog
        val javaLangTarget = libs.getJavaLangTarget(kotlinConfig)
        compileOptions {
            if (!javaLangTarget.isNullOrEmpty()) {
                JavaVersion.toVersion(javaLangTarget).let {
                    sourceCompatibility = it
                    targetCompatibility = it
                }
            }

            /*
             * Enable support for new language APIs.
             * https://developer.android.com/studio/write/java8-support
             * https://developer.android.com/studio/write/java8-support-table
             * https://jakewharton.com/androids-java-8-support/
             * https://jakewharton.com/androids-java-9-10-11-and-12-support/
             */
            val desugaringEnabled by project.isDesugaringEnabled()
            if (desugaringEnabled) {
                isCoreLibraryDesugaringEnabled = true

                libs.onLibrary("android-desugarLibs") {
                    dependencies.add("coreLibraryDesugaring", it)
                }
            }
        }
        if (!javaLangTarget.isNullOrEmpty()) {
            kotlinOptions(project) {
                jvmTarget = javaLangTarget
            }
        }

        if (this is ApplicationExtension) {
            bundle {
                abi.enableSplit = true
                density.enableSplit = true
                language.enableSplit = false
            }

            dependenciesInfo {
                // Dependency metadata in the signature block
                // No need to give Google Play more info about the app.
                includeInApk = false
                includeInBundle = false
            }
        }

        // Disable all features by default
        val composeEnabled = config.setupCompose || setupCompose
        buildFeatures.apply {
            aidl = false
            compose = composeEnabled
            prefab = false
            renderScript = false
            resValues = false
            shaders = false
            viewBinding = false

            buildConfig = config.enableBuildConfig ?: enableBuildConfig ?: false
        }

        // Set compose compiler version
        libs.onVersion(ALIAS_ANDROIDX_COMPOSE_COMPILER) {
            composeOptions.kotlinCompilerExtensionVersion = it
        }

        setupPackagingOptions(project)

        dependencies.setupAndroidDependencies(
            project = project,
            isApplication = this is ApplicationExtension,
            setupRoom = setupRoom,
            setupCompose = composeEnabled,
            kotlinConfig = kotlinConfig,
        )

        // Custom settings
        config.configurator?.invoke(this, project)
    }

    if (!project.isGenericCompilationEnabled || disableTests().get()) {
        tasks.withType<AndroidLintTask> { enabled = false }
        tasks.withType<AndroidLintTextOutputTask> { enabled = false }
    }
}

private fun CommonExtension<*, *, *, *>.setupAndroidBasic(
    config: AndroidConfigSetup,
    project: Project,
    setupRoom: Boolean,
    setupKsp: Boolean,
) {
    config.buildToolsVersion?.let {
        buildToolsVersion = it
    }

    val isMaxDebug by project.isMaxDebugEnabled()
    if (isMaxDebug && !config.previewSdkVersion.isNullOrEmpty()) {
        compileSdkPreview = config.previewSdkVersion
    } else {
        compileSdk = config.compileSdkVersion
    }

    setupDefaultConfig(config, project, setupRoom = setupRoom, setupKsp = setupKsp)

    testOptions.unitTests {
        // Required for Robolectric
        isIncludeAndroidResources = true
        isReturnDefaultValues = true

        all { it.useJUnit() }
    }

    buildTypes {
        maybeCreate("debug").apply {
            matchingFallbacks.addAll(listOf("test", "debug", "qa"))

            val isCI by project.isCI()
            val isRelease by project.isRelease()

            // UI localization testing.
            // Generate resources for pseudo-locales: en-XA and ar-XB
            if (isMaxDebug && !isRelease && !isCI) {
                isPseudoLocalesEnabled = true
            }
        }

        maybeCreate("release").apply {
            matchingFallbacks.addAll(listOf("release", "prod", "production"))
        }
    }
}

private fun CommonExtension<*, *, *, *>.setupDefaultConfig(
    config: AndroidConfigSetup,
    project: Project,
    setupRoom: Boolean,
    setupKsp: Boolean,
) = defaultConfig {
    minSdk = config.minSdkVersion

    val isCI by project.isCI()
    val isRelease by project.isRelease()
    val isMaxDebug by project.isMaxDebugEnabled()

    val targetSdk: Int = when {
        isMaxDebug && !isRelease -> config.lastSdkVersion
        else -> config.targetSdkVersion
    }
    (this as? ApplicationBaseFlavor)?.targetSdk = targetSdk
    @Suppress("DEPRECATION") // Will be removed from AGP DSL in v9.0
    (this as? LibraryBaseFlavor)?.targetSdk = targetSdk

    // Don't rasterize vector drawables (androidMinSdk >= 21)
    @Suppress("MagicNumber")
    if (config.minSdkVersion >= 21) {
        vectorDrawables.generatedDensities()
    }

    // en_XA and ar_XB are pseudo-locales for debugging.
    // The rest of the locales provides an explicit list of the languages to keep in the
    // final app.
    // Doing this strips out extra locales from libraries like
    // Google Play Services and Firebase, which add unnecessary bloat.
    // https://developer.android.com/studio/build/shrink-code#unused-alt-resources
    // https://gist.github.com/amake/0ac7724681ac1c178c6f95a5b09f03ce
    // https://stackoverflow.com/questions/42937870/what-does-b-stand-for-and-what-is-the-syntax-behind-bsrlatn
    // https://stackoverflow.com/a/49117551/1816338
    val languages = config.languages.toMutableList().apply {
        if (isMaxDebug && !isRelease && !isCI) addAll(arrayOf("en_XA", "ar_XB"))
    }
    resourceConfigurations.addAll(languages)

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    // testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"

    if (setupRoom) {
        // Exported Room DB schemas
        // Enable incremental compilation for Room
        val roomSchemasDir = "${project.projectDir}/schemas"
        project.ksp {
            arg("room.generateKotlin", "true")
            arg("room.incremental", "true")
            arg("room.schemaLocation", roomSchemasDir)
        }
        // Add exported schema location as test app assets.
        sourceSets["androidTest"].assets.srcDir(roomSchemasDir)
    }

    // KSP generated sources
    if (setupKsp || setupRoom) {
        sourceSets["main"].apply {
            kotlin.srcDir("build/generated/ksp/main/kotlin")
            resources.srcDir("build/generated/ksp/main/resources")
        }
        sourceSets["test"].kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

private fun CommonExtension<*, *, *, *>.setupPackagingOptions(project: Project) {
    packagingOptions {
        val isCI by project.isCI()
        val isRelease by project.isRelease()

        resources.pickFirsts += listOf(
            // byte-buddy-agent vs kotlinx-coroutines-debug conflict
            "**/attach_hotspot_windows.dll",
        )
        // remove all unneeded files from the apk/bundle
        resources.excludes += listOfNotNull(
            "**-metadata.json",
            "**-metadata.properties",
            "**.readme",
            "**/**-metadata.properties",
            "**/**version.txt",
            "**/*-ktx.version",
            "**/CertPathReviewerMessages_de.properties",
            "**/DebugProbesKt.bin",
            "**/app-metadata.properties",
            "**/org/apache/commons/**",
            "**/previous-compilation-data.bin", // See https://github.com/Kotlin/kotlinx.coroutines/issues/3668
            "**/version.properties",
            "*.txt",
            "META-INF/**.properties",
            "META-INF/CHANGES**",
            "META-INF/DEPENDENCIES**",
            "META-INF/LICENSE**",
            "META-INF/MANIFEST**",
            "META-INF/NOTICE**",
            "META-INF/README**",
            "META-INF/licenses/**",
            "META-INF/native-image/**",
            "META-INF/{AL2.0,LGPL2.1,beans.xml}",
            "jni/**",
            "jsr305_annotations/**",
            "okhttp3/**",
            "res/**/keep.xml",

            // Required for kotlin serialization
            // "**/*.kotlin_*",

            // /com/google/api/client/googleapis/google-api-client.properties
            // required for GoogleUtils!
            "*-*.properties",
            "*.properties",
            "firebase-**.properties",
            "play-services-**.properties",
            "protolite-**.properties",
            "transport-**.properties",
            "vision-**.properties",
            "{barcode-scanning-common,build-data,common,image}.properties",

            // Required for Compose Layout Inspector (see b/223435818)
            if (isCI || isRelease) "META-INF/**.version" else null,
        )

        // Release-only packaging options (https://issuetracker.google.com/issues/155215248#comment5)
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.apply {
            onVariants(selector().withBuildType("release")) {
                it.packaging.resources.excludes.add("META-INF/**.version")
            }
        }
    }
}

private fun CommonExtension<*, *, *, *>.kotlinOptions(
    project: Project,
    configure: Action<KotlinJvmOptions>,
) {
    val extensions = (this as ExtensionAware).extensions
    project.plugins.withId("org.jetbrains.kotlin.android") {
        try {
            extensions.configure("kotlinOptions", configure)
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            project.logger.error("$e", e)
        }
    }
}

private fun Project.ksp(configure: Action<KspExtension>) {
    extensions.configure("ksp", configure)
}

private val PluginAware.hasKsp: Boolean get() = plugins.hasPlugin("com.google.devtools.ksp")

private fun Project.setupSigning(appExtension: BaseAppModuleExtension) {
    // Configured signing dynamically with signing.properties file
    appExtension.signingConfigs {
        val debug = get("debug")
        debug.enableMaxSigning()

        var releaseConfigured = false
        // FIXME: Extract as a AGP plugin
        // FIXME: Better gradle conf cache support?
        var signPropsFile = rootProject.file("signing.properties")
        if (!signPropsFile.canRead()) {
            signPropsFile = project.file("signing.properties")
        }

        if (signPropsFile.canRead()) {
            val properties = Properties()
            signPropsFile.inputStream().use { properties.load(it) }

            var alias = properties["keyAlias"]?.toString()
            if (!alias.isNullOrEmpty()) {
                releaseConfigured = alias == "release"
                maybeCreate(alias).apply {
                    logger.lifecycle("> Conf :signing: '$alias' configuration loaded from properties")
                    keyAlias = alias
                    storeFile = getKeystoreFile(properties["keystorePath"])
                    val password = properties["keystorePassword"]?.toString()
                    storePassword = password
                    keyPassword = properties["keyPassword"]?.toString()
                        .orEmpty().ifEmpty { password }
                    enableMaxSigning()
                }
            }
            if (!releaseConfigured) {
                alias = properties["releaseKeyAlias"]?.toString()
                if (!alias.isNullOrEmpty()) {
                    releaseConfigured = true
                    create("release") {
                        logger.lifecycle("> Conf :signing: 'release' configuration loaded from properties")
                        keyAlias = alias
                        storeFile = getKeystoreFile(properties["releaseKeystorePath"])
                        val password = properties["releaseKeystorePassword"]?.toString()
                        storePassword = password
                        keyPassword = properties["releaseKeyPassword"]?.toString()
                            .orEmpty().ifEmpty { password }
                        enableMaxSigning()
                    }
                }
            }
        }

        // prefill release configuration (at least with debug signing)
        if (!releaseConfigured) {
            create("release") {
                logger.lifecycle("> Conf :signing: 'release' configuration copied from 'debug'")
                storeFile = debug.storeFile
                keyAlias = debug.keyAlias
                storePassword = debug.storePassword
                keyPassword = debug.keyPassword
                enableMaxSigning()
            }
        }
    }
}

private fun Project.getKeystoreFile(rawPath: Any?): File {
    val keystorePath = rawPath.toString()
    var file = rootProject.file(keystorePath).absoluteFile
    if (!file.canRead() || !file.exists()) {
        file = project.file(keystorePath).absoluteFile
    }
    return file
}

private fun ApkSigningConfig.enableMaxSigning() {
    enableV1Signing = true
    enableV2Signing = true
    try {
        enableV3Signing = true
        enableV4Signing = true
    } catch (_: Throwable) {
    }
}


internal fun DependencyHandler.setupAndroidDependencies(
    project: Project,
    isApplication: Boolean,
    setupRoom: Boolean,
    setupCompose: Boolean,
    kotlinConfig: KotlinConfigSetup,
) {
    val libs = project.libsCatalog

    compileOnlyWithConstraint(JSR305_DEPENDENCY)
    libs.onLibrary("androidx-annotation") { compileOnlyWithConstraint(it) }
    libs.onLibrary("androidx-annotation-experimental") { compileOnlyWithConstraint(it) }
    libs.onLibrary("jetbrains-annotation") { compileOnlyWithConstraint(it) }

    implementation(kotlin("stdlib-jdk7"))
    androidTestImplementation(kotlin("test-junit"))

    if (kotlinConfig.setupCoroutines) {
        libs.onLibrary("kotlinx-coroutines-debug") { debugCompileOnly(it) }
        libs.onLibrary("kotlinx-coroutines-test") {
            androidTestImplementation(it) {
                // https://github.com/Kotlin/kotlinx.coroutines/tree/ca14606/kotlinx-coroutines-debug#debug-agent-and-android
                exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-debug")
            }
        }
    }

    if (setupCompose) {
        // Support compose @Stable and @Immutable annotations
        libs.onLibrary("androidx-compose-runtime") { compileOnlyWithConstraint(it) }
        // Support compose @Preview
        libs.onLibrary("androidx-compose-ui-tooling-preview") { compileOnlyWithConstraint(it) }
        // Support Layout inspector and any other tools
        libs.onLibrary("androidx-compose-ui-tooling") { debugImplementation(it) }
        // Experimental composition tracing
        // https://developer.android.com/jetpack/compose/performance/tracing
        libs.onLibrary("androidx-compose-tracing") { debugImplementation(it) }
        // Tests
        libs.onLibrary("androidx-compose-ui-test") { androidTestImplementation(it) }
        libs.onLibrary("androidx-compose-ui-test-junit4") { androidTestImplementation(it) }
        libs.onLibrary("androidx-compose-ui-test-manifest") { androidTestImplementation(it) }
    }

    if (isApplication) {
        libs.onLibrary("androidx-activity") { implementation(it) }
        libs.onLibrary("androidx-lifecycle-runtime") { implementation(it) }
        libs.onLibrary("androidx-profileInstaller") { runtimeOnly(it) }

        if (setupCompose) {
            // BackHandler, setContent, ReportDrawn, rememberLauncherForActivityResult, and so on.
            libs.onLibrary("androidx-activity-compose") { implementation(it) }
        }

        libs.onLibrary("square-leakcanary") { debugImplementation(it) }
        libs.onLibrary("square-plumber") { implementation(it) }

        libs.onLibrary("flipper") { flipper ->
            debugImplementation(flipper)
            libs.onLibrary("flipper-leakcanary2") { debugImplementation(it) }
            libs.onLibrary("flipper-network") { debugImplementation(it) }
            libs.onLibrary("flipper-soloader") { debugImplementation(it) }
        }
    }

    libs.onLibrary("test-jUnit") { testImplementation(it) }
    libs.onLibrary("test-mockito-core") { testImplementation(it) }
    libs.onLibrary("test-robolectric") { testImplementation(it) }

    libs.onLibrary("androidx-arch-core-testing") { testImplementation(it) }
    libs.onLibrary("androidx-test-core") { testImplementation(it) }
    libs.onLibrary("androidx-test-core-ktx") { testImplementation(it) }
    libs.onLibrary("androidx-test-junit") { testImplementation(it) }

    libs.onLibrary("androidx-test-core") { androidTestImplementation(it) }
    libs.onLibrary("androidx-test-core-ktx") { androidTestImplementation(it) }
    libs.onLibrary("androidx-test-espresso-core") { androidTestImplementation(it) }
    libs.onLibrary("androidx-test-espresso-idling") { androidTestImplementation(it) }
    libs.onLibrary("androidx-test-junit") { androidTestImplementation(it) }
    libs.onLibrary("androidx-test-rules") { androidTestImplementation(it) }
    libs.onLibrary("androidx-test-runner") { androidTestImplementation(it) }

    libs.onLibrary("firebase-bom") { implementation(enforcedPlatform(it)) }
    libs.onLibrary("androidx-compose-bom") { implementation(platform(it)) }

    if (setupRoom) {
        libs.onLibrary("androidx-room-compiler") { ksp(it) }
        libs.onLibrary("androidx-room-runtime") { implementation(it) }
        libs.onLibrary("androidx-room-testing") { testImplementation(it) }
        libs.onLibrary("androidx-room-common") { compileOnlyWithConstraint(it) }

        libs.onLibrary("androidx-room-paging") { implementation(it) }

        if (kotlinConfig.setupCoroutines) {
            libs.onLibrary("androidx-room-ktx") { implementation(it) }
        }
    }

    constraints {
        libs.onBundle("androidx") { implementation(it) }
        libs.onBundle("accompanist") { implementation(it) }
        libs.onBundle("android-common") { implementation(it) }
        libs.onBundle("gms") { implementation(it) }
        libs.onBundle("kotlinx") { implementation(it) }
        libs.onBundle("koin") { implementation(it) }
        libs.onBundle("common") { implementation(it) }
    }
}

private fun CommonExtension<*, *, *, *>.onFinalizeDsl(project: Project) {
    val buildConfigIsRequired = buildFeatures.buildConfig == true || buildTypes.any {
        /** @see com.android.build.gradle.internal.dsl.BuildType */
        (it as BaseConfig).buildConfigFields.isNotEmpty()
    }
    if (buildConfigIsRequired) {
        buildFeatures.buildConfig = true
    }

    val libs = project.libsCatalog
    val isApplication = this is ApplicationExtension
    val enableMaxDebug = project.isMaxDebugEnabled().get().toString()
    for (bt in buildTypes) {
        val isReleaseBuildType = bt.name == "release"

        // Add leakcanary to all build types in the app
        if (isApplication && !isReleaseBuildType && bt.name != "debug") {
            libs.onLibrary("square-leakcanary") {
                project.dependencies.add("${bt.name}Implementation", it)
            }
        }

        if (buildConfigIsRequired) {
            val boolean = "boolean"
            val enableTest = if (isReleaseBuildType) enableMaxDebug else "true"
            bt.buildConfigField(boolean, "TEST", enableTest)
            bt.buildConfigField(boolean, "MAX_DEBUG", enableMaxDebug)
        }
    }
}

internal const val ALIAS_ANDROIDX_COMPOSE_COMPILER = "androidx-compose-compiler"

// Old but sometimes useful annotation lib
// Doesn't update, so no need for the version catalog.
internal const val JSR305_DEPENDENCY = "com.google.code.findbugs:jsr305:3.0.2"
