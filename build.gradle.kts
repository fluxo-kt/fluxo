@file:Suppress("SpreadOperator")

import java.net.URI
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

// Security pins (catalog bundle `pinned-security`) for the classes the build actually runs. The harness pins the
// same bundle with `eachDependency`, but it registers them inside its own apply(), after `plugins {}`
// has resolved and loaded the plugin classpath, so they only change later re-resolutions such as
// `buildEnvironment` and the dependency-guard snapshot: those reports show the pins while the root script's
// classloader still loads the unpinned jars. `buildscript {}` runs before `plugins {}`, so these constraints
// reach the real classpath. Constraints only raise versions.
buildscript {
    dependencies {
        constraints {
            libs.bundles.pinned.security.get().forEach { add("classpath", "${it.module}:${it.version}") }
        }
    }
}

plugins {
    alias(libs.plugins.android.lib) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.dokka) apply false
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.fluxo.bcv.js) apply false
    // `apply false`, but declared here so the publication plugin loads into the root
    // plugin classloader the harness shares — the harness is compiled `compileOnly`
    // against vanniktech and configures MavenPublishBaseExtension reflectively, so the
    // classes must be visible to it. Library modules then apply it from this classpath.
    alias(libs.plugins.vanniktech.mvn.publish) apply false
    alias(libs.plugins.fluxo.kmp.conf)
}

val isRelease by isRelease()

// JitPack (jitpack.yml) builds a commit on request and serves what lands in ~/.m2 as
// com.github.fluxo-kt.fluxo:<module>:<commit>. Gradle module metadata links every platform variant by group and
// version, so a JitPack build must publish under exactly those coordinates, not the Maven Central ones.
val jitpack = providers.environmentVariable("JITPACK").orNull == "true"

// Setup project defaults.
fkcSetupRaw {
    explicitApi()

    // Default KMP setup.
    defaults {
        // The target groups add only what the running Kotlin fully supports, so they skip the Intel targets
        // Kotlin 2.4.20 deprecates and never add iosX64. Fluxo keeps publishing those four until Kotlin removes them:
        // dropping a target takes its artefacts away from consumers, so it waits for the Kotlin version that forces it.
        allDefaultTargets()
        iosX64()
        // Kotlin 2.5 (the pre-release build) removes these three, and an explicit call to a removed target fails the
        // build; iosX64 is still accepted there (2.5.0-Beta1).
        if (!providers.gradleProperty("fluxo.kotlin").isPresent) {
            macosX64()
            tvosX64()
            watchosX64()
        }
        // Not in the default groups: publishes fluxo for WASI runtimes, which need no JS environment.
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
        wasmWasi()
    }

    projectName = "Fluxo"
    description = "Kotlin Multiplatform MVI / MVVM+ framework"
    githubProject = "fluxo-kt/fluxo"
    fun env(name: String) = providers.environmentVariable(name).get()
    group = if (jitpack) "${env("GROUP")}.${env("ARTIFACT")}" else "io.github.fluxo-kt"
    // JitPack passes a commit-derived VERSION even for a `<branch>-SNAPSHOT` request (built as `dev-<commit>-1`, then
    // served rewritten to `dev-SNAPSHOT`), so the harness's SNAPSHOT-version rewrite never applies here.
    if (jitpack) version = env("VERSION")

    // Publish all library modules to Maven Central (Central Portal) via vanniktech.
    // POM name/URL/SCM derive from projectName/githubProject/group above; license
    // defaults to Apache-2.0 (matches LICENSE). Mirrors the sibling harness config.
    enablePublication = true
    publicationConfig {
        developerId = "amal"
        developerName = "Art Shendrik"
        developerEmail = "artyom.shendrik@gmail.com"
    }
    // Release javadoc jars carry Dokka HTML API docs. The harness wires Dokka into the javadoc jar only for
    // non-SNAPSHOT versions (the output is large); snapshots publish an empty javadoc jar, which Central accepts.
    // Not on JitPack: its commit version counts as a release, and the source links would name a nonexistent tag.
    useDokka = !jitpack

    enableSpotless = true
    enableApiValidation = true
    apiValidation {
        tsApiChecks = true
        klibValidationEnabled = true
    }

    allWarningsAsErrors = true
    // Compile and test on the JDK of the bytecode target (javaLangTarget), not on the Gradle daemon's JDK:
    // the daemon runs a newer JDK (gradle/gradle-daemon-jvm.properties), and without a toolchain kotlinc
    // would compile against its class library and JVM tests would run on it instead of the consumer floor.
    setupJvmToolchain = true
    // No worker-thread copy of the native tests (`<target>BackgroundTest`, -trw): they catch code bound to the main
    // thread or to thread-local state, and fluxo's sources use neither (no @ThreadLocal, main dispatcher or
    // main-thread API), so they would only add native link and test time (+42% on a full local native test run).
    // Turn this on when fluxo gains such code, e.g. a Dispatchers.Main or Apple main-queue integration.
    backgroundNativeTests = false
    useIndyLambdas = isRelease
    optInInternal = true
    optIns = listOf(
        "kotlin.js.ExperimentalJsExport",
    )

    // TODO: BinaryCompatibilityValidatorConfig.disableForNonRelease = true
}

dependencies {
    kover(projects.fluxoCommon)
    kover(projects.fluxoCore)
    kover(projects.fluxoData)
}

kover {
    // TODO: Disable Kover by default to reduce performance penalty.
    //  https://github.com/Kotlin/kotlinx-kover/issues/531#issuecomment-1929483468

    val isCI by isCI()
    reports {
        total {
            xml {
                onCheck = true
                xmlFile = layout.buildDirectory.file("reports/kover-merged-report.xml")
            }
            html {
                onCheck = !isCI && isRelease
                htmlDir = layout.buildDirectory.dir("reports/kover-merged-report-html")
            }

            verify {
                onCheck = true
                rule {
                    groupBy = kotlinx.kover.gradle.plugin.dsl.GroupingEntityType.APPLICATION
                    minBound(50)
                    bound {
                        minValue = 72
                        coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                        aggregationForGroup = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                    }
                    bound {
                        minValue = 65
                        coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.INSTRUCTION
                        aggregationForGroup = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                    }
                    bound {
                        minValue = 50
                        coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH
                        aggregationForGroup = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                    }
                }
            }
        }
        filters {
            excludes {
                classes(
                    *listOfNotNull(
                        // Test classes
                        "kt.fluxo.test.*",
                        "kt.fluxo.tests.*",
                        // Inline DSL with coverage not detected in release mode.
                        if (isRelease) "kt.fluxo.core.FluxoKt*" else null,
                        if (isRelease) "kt.fluxo.core.dsl.MigrationKt*" else null,
                    ).toTypedArray(),
                )

                if (isRelease) {
                    annotatedBy(
                        // Coverage is invalid for inline and InlineOnly methods in release mode.
                        "*Inline*",
                        // No real need for a deprecated methods' coverage.
                        "*Deprecated*",
                        // JvmSynthetic used as a marker
                        // for migration helpers hidden from non-kotlin usage
                        // and not supposed for coverage.
                        "*Synthetic*",
                    )
                }
            }
        }
    }
}

// Drift gate: rejects deprecated/removed/no-op Gradle-properties keys with rationale.
// Parsing via java.util.Properties — regex would mishandle line continuations + escapes
// that real .properties files honour. `@CacheableTask` because the inputs are
// declarative; PathSensitivity.NONE because we read by file content, not path.
@CacheableTask
abstract class CheckForbiddenFlagsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val propertiesFile: RegularFileProperty

    @get:Input
    abstract val forbidden: MapProperty<String, String>

    // Keys whose value is a prefix-match — handles families like
    // `android.defaults.buildfeatures.*` (AGP 9, AGENTS.md gotcha #21) without
    // enumerating each leaf.
    @get:Input
    abstract val forbiddenPrefixes: MapProperty<String, String>

    @TaskAction
    fun verify() {
        // ISO-8859-1 is the canonical .properties encoding per java.util.Properties spec.
        val props = java.util.Properties()
        propertiesFile.get().asFile.bufferedReader(Charsets.ISO_8859_1).use { props.load(it) }
        val violations = mutableListOf<Pair<String, String>>()
        val present: Set<String> = props.stringPropertyNames()
        for ((key, rationale) in forbidden.get()) {
            if (key in present) violations += key to rationale
        }
        for ((prefix, rationale) in forbiddenPrefixes.get()) {
            for (key in present) {
                if (key.startsWith(prefix)) violations += key to rationale
            }
        }
        if (violations.isNotEmpty()) {
            val msg = buildString {
                appendLine("Forbidden Gradle properties detected in ${propertiesFile.get().asFile}:")
                appendLine()
                for ((key, rationale) in violations) {
                    appendLine("  - `$key` = `${props[key]}`")
                    appendLine("    $rationale")
                }
                appendLine()
                appendLine("Remove these keys. The rationale per key documents why each is no-op,")
                appendLine("deprecated, removed upstream, or actively harmful at the current toolchain.")
            }
            throw GradleException(msg)
        }
    }
}

val checkForbiddenFlags = tasks.register<CheckForbiddenFlagsTask>("checkForbiddenFlags") {
    description = "Rejects deprecated/removed/no-op Gradle-properties keys; drift gate."
    group = "verification"
    propertiesFile.set(layout.projectDirectory.file("gradle.properties"))
    forbidden.putAll(
        mapOf(
            "kotlin.native.binary.memoryModel" to
                "Deprecated since Kotlin 1.7.20; the new MM is the only one supported in K2.x. Setting this key is a no-op that misleads readers.",
            "kotlin.mpp.androidSourceSetLayoutVersion" to
                "Only layout v2 supported in K2.x; the property is silently ignored.",
            "kotlin.compiler.preciseCompilationResultsBackup" to
                "Always-on in K2.x; the property is silently ignored.",
            "kotlin.incremental.useClasspathSnapshot" to
                "Always-on in K2.x; the property is silently ignored.",
            "kotlin.mpp.import.enableKgpDependencyResolution" to
                "Removed in K2.x; KGP's own dependency resolution is now the only path.",
            "kotlin.mpp.stability.nowarn" to
                "KMP went stable in Kotlin 1.9; the suppression flag is obsolete.",
            "org.gradle.unsafe.watch-fs" to
                "Replaced by stable `org.gradle.vfs.watch` since Gradle 7.x; the unsafe variant is removed in Gradle 9.",
            "org.gradle.configureondemand" to
                "Deprecated in Gradle 9; incompatible with the current configuration-cache invariants.",
            "android.useAndroidX" to
                "Default-on since AGP 7.x; setting it is a no-op and clutters the file.",
            "android.enableJetifier" to
                "Default-off since AGP 7.x; enabling it harms build time for ~zero benefit on a clean AndroidX codebase.",
        ),
    )
    // AGP 9 rejects global build-feature defaults — configure per-module DSL instead
    // (AGENTS.md gotcha #21). Also rejects experimental Android lint/resource flags.
    forbiddenPrefixes.putAll(
        mapOf(
            "android.defaults.buildfeatures." to
                "AGP 9 rejects global build-feature defaults; configure per-module via the `android { buildFeatures { … } }` DSL.",
            "android.experimental." to
                "Lint/resource experimental flags are not honoured under AGP 9; configure per-module DSL or drop.",
        ),
    )
}

tasks.named("check") {
    dependsOn(checkForbiddenFlags)
}

// Sigstore keyless signing relies on the GitHub Actions OIDC token issuer
// (`id-token: write`), only granted to `release.yml` on `v*` tags. On any other
// invocation (PR/snapshot/local) `sigstoreSign*Publication` fails with "Failed to
// obtain signing certificate" because no OIDC issuer is reachable. The plugin
// auto-attaches a task per MavenPublication, so disable them up-front when the
// release-publish task isn't even in the start parameters — self-aware, no env
// discipline needed (mirrors the pattern in `settings.gradle.kts`).
val isReleasePublish = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("publishAndReleaseToMavenCentral", ignoreCase = true)
}
if (!isReleasePublish) {
    subprojects {
        pluginManager.withPlugin("dev.sigstore.sign") {
            tasks.matching { it.name.startsWith("sigstoreSign") }.configureEach {
                enabled = false
            }
        }
    }
}

// Read here, not inside allprojects {}: the catalog accessor is a root-script member.
val kotlinStdlibJs = libs.kotlin.stdlib.js.get().let { "${it.module}:${it.version}" }

allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.name == "kotlin-dom-api-compat") {
                // Exclude unused DOM API.
                useTarget(kotlinStdlibJs)
            }
        }
    }

    // FIXME: Setup automatically.
    plugins.withType<org.jetbrains.dokka.gradle.DokkaPlugin> {
        extensions.configure<org.jetbrains.dokka.gradle.DokkaExtension> {
            dokkaSourceSets {
                configureEach {
                    if (name.startsWith("ios")) {
                        displayName.set("ios")
                    }

                    sourceLink {
                        localDirectory.set(rootDir)
                        // Releases link to their vX.Y.Z tag (RELEASING.md), snapshots to main, the branch they publish from.
                        // Lazy: the harness assigns the project version during configuration. Dokka runs only for
                        // release versions (useDokka above), so the snapshot branch is a fallback, not a live path.
                        remoteUrl.set(
                            provider {
                                val projectVersion = version.toString()
                                val ref = if (projectVersion.endsWith("-SNAPSHOT")) "main" else "v$projectVersion"
                                URI("https://github.com/fluxo-kt/fluxo/blob/$ref")
                            },
                        )
                        remoteLineSuffix.set("#L")
                    }
                }
            }
        }
    }
}

// Yarn resolutions for vulnerable transitive npm packages of KGP's JS test tooling (mocha, webpack). Each is
// the newest release in the major line the lock already uses, at or above every advisory's first patched
// version; delete an entry once KGP's bundled tooling pulls a patched version by itself. Advisories
// (GitHub, read 2026-10-05): brace-expansion 2.x < 2.1.7, js-yaml 4.x < 4.3.2, diff 6–8 < 8.0.3,
// serialize-javascript < 7.1.2. diff and serialize-javascript cross a major from what mocha requests, and
// brace-expansion from what Karma's minimatch requests (1.x); Node and browser JS tests pass with them. mocha itself stays at KGP's bundled version: mocha 12 breaks KGP's test reporter (zero tests
// run). On a resolution-only change KGP considers build/js/package.json up to date and leaves it stale, so
// `./updateBaselines` deletes build/js and reruns every npm task before upgrading the lock (deleting alone was
// not enough once: the producers still reported UP-TO-DATE).
// No resolution can fix braces <= 3.0.3 (GHSA-vfj7-8cjw-p6xm, DoS on deeply nested patterns), reached only via
// KGP's Karma fork and its file watcher: no patched version exists (read 2026-10-07), so Dependabot alert #248
// is dismissed as tolerable risk (test-only; the patterns are this build's own). Once the advisory lists a
// patched version, add a resolution here and reopen the alert.
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        resolution("brace-expansion", "2.1.7")
        resolution("js-yaml", "4.3.2")
        resolution("diff", "8.0.4")
        resolution("serialize-javascript", "7.1.2")
    }
}

// Dependency verification for toolchain archives of OTHER hosts.
// Kotlin/Native, Node.js and Binaryen are downloaded as host-specific archives, so a regen of
// `gradle/verification-metadata.xml` on one OS records only that host's archives, and strict
// verification then fails on every other OS (CI runs macOS, Ubuntu and Windows). Resolving the
// other hosts' archives through Gradle while `--write-verification-metadata` runs records their real
// checksums, which replaces hand-pinning hashes from upstream SHASUMS files on every toolchain bump.
// Run it in the same invocation as the metadata write; `./updateBaselines` does.
tasks.register("resolveCrossHostToolchains") {
    description = "Resolves every host's Kotlin/Native, Node.js and Binaryen archive so verification metadata covers all hosts."
    group = "verification"
    notCompatibleWithConfigurationCache("resolves detached configurations through temporary repositories")
    val kotlinVersion = org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion(logger)
    doLast {
        // The env specs live on whichever projects apply the JS/Wasm targets, not necessarily the root.
        val nodeVersions = allprojects.flatMap {
            listOfNotNull(
                it.extensions.findByType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>()?.version?.orNull,
                it.extensions.findByType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>()?.version?.orNull,
            )
        }.toSet()
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
        val binaryenVersions = allprojects.mapNotNull {
            it.extensions.findByType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>()?.version?.orNull
        }.toSet()

        fun resolve(coordinates: List<String>): Int {
            val deps = coordinates.map { dependencies.create(it) }.toTypedArray()
            return configurations.detachedConfiguration(*deps).apply { isTransitive = false }.resolve().size
        }

        // Kotlin/Native prebuilt archives come from Maven Central (settings repositories). Resolve them
        // BEFORE adding the temporary repositories below: any project-level repository makes Gradle ignore
        // the settings repositories for this project.
        val konanHosts = listOf("linux-x86_64@tar.gz", "macos-aarch64@tar.gz", "macos-x86_64@tar.gz", "windows-x86_64@zip")
        val konan = resolve(konanHosts.map { "org.jetbrains.kotlin:kotlin-native-prebuilt:$kotlinVersion:$it" })

        // Same repository layouts KGP uses for its own downloads.
        val temporary = listOf(
            repositories.ivy {
                url = uri("https://nodejs.org/dist")
                patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                metadataSources { artifact() }
                content { includeModule("org.nodejs", "node") }
            },
            repositories.ivy {
                url = uri("https://github.com/WebAssembly/binaryen/releases/download")
                patternLayout { artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]") }
                metadataSources { artifact() }
                content { includeModule("com.github.webassembly", "binaryen") }
            },
        )
        try {
            val nodePlatforms = listOf("darwin-arm64@tar.gz", "darwin-x64@tar.gz", "linux-arm64@tar.gz", "linux-x64@tar.gz", "win-x64@zip")
            val binaryenPlatforms = listOf("aarch64-linux", "arm64-macos", "arm64-windows", "x86_64-linux", "x86_64-macos", "x86_64-windows")
            // One configuration per version: within a single configuration, conflict resolution would keep only the
            // highest version of a module and silently skip the others (JS and Wasm can pin different Node versions).
            val node = nodeVersions.sumOf { v -> resolve(nodePlatforms.map { "org.nodejs:node:$v:$it" }) }
            val binaryen = binaryenVersions.sumOf { v -> resolve(binaryenPlatforms.map { "com.github.webassembly:binaryen:$v:$it@tar.gz" }) }
            logger.lifecycle("Resolved cross-host toolchain archives: Kotlin/Native $konan, Node.js $node $nodeVersions, Binaryen $binaryen $binaryenVersions")
            // A missing archive already fails resolution; zero can only mean version discovery found nothing (a KGP
            // API change), which would leave the other CI hosts' archives unpinned and fail them under strict mode.
            check(konan > 0 && node > 0 && binaryen > 0) { "No cross-host archives resolved for a toolchain kind: fix the version discovery above" }
        } finally {
            repositories.removeAll(temporary.toSet())
        }
    }
}
