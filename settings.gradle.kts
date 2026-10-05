@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }

    // Dogfood the in-repo harness locally; on CI — or any fresh checkout without the sibling —
    // resolve the PUBLISHED io.github.fluxo-kt.fluxo-kmp-conf plugin, so CI builds exactly what
    // external consumers get (invariant I1). Computed inside pluginManagement because that block is
    // evaluated before the settings-script body — a top-level val would be out of scope here.
    // providers.*/settingsDir keep it configuration-cache-correct under strict CC.
    //
    // Baselining tasks (verification-metadata, dependency-guard) MUST capture the *published*
    // plugin graph; composite mode resolves the harness from the filesystem, skips Plugin Portal,
    // and produces baselines that miss the published JAR + transitive POMs (or the harness's own
    // classpath coord on the root `:dependencyGuard`) — which then blows up every CI job on the
    // next push (AGENTS.md gotcha #29). Making the predicate self-aware on baseline-write tasks
    // structurally eliminates the human-discipline-around-a-flag failure mode for the whole class.
    // Public StartParameter API — `writeDependencyVerifications` (plural, no "Metadata" suffix)
    // backs `--write-verification-metadata <sha256,…>` and is empty when no checksums were
    // requested. Verified against gradle-start-parameter-9.6.0.jar.
    val isWritingVerificationMetadata =
        gradle.startParameter.writeDependencyVerifications.isNotEmpty()

    // gradle.properties makes verification lenient for local builds; CI must verify strictly, which
    // only a -D/command-line override can do (the mode is fixed before this script runs). Fail a CI
    // build that forgot the override instead of letting it silently accept unpinned artefacts.
    // Writing metadata is exempt: verification does not gate a regeneration.
    if (providers.environmentVariable("CI").orNull?.toBooleanStrictOrNull() == true &&
        !isWritingVerificationMetadata &&
        gradle.startParameter.dependencyVerificationMode !=
        org.gradle.api.artifacts.verification.DependencyVerificationMode.STRICT
    ) {
        throw GradleException(
            "CI builds must verify dependencies strictly: add " +
                "-Dorg.gradle.dependency.verification=strict to GRADLE_OPTS (see gradle.properties)."
        )
    }
    val isWritingDepGuardBaseline = gradle.startParameter.taskNames.any { taskName ->
        // Match both `:<subproject>:dependencyGuardBaseline` and bare `dependencyGuardBaseline`.
        taskName.endsWith("dependencyGuardBaseline", ignoreCase = true)
    }
    val isWritingBaseline = isWritingVerificationMetadata || isWritingDepGuardBaseline
    val useLocalHarness = !isWritingBaseline && (
        providers.gradleProperty("fluxo.dogfood").orNull?.toBooleanStrictOrNull()
            ?: (providers.environmentVariable("CI").orNull?.toBooleanStrictOrNull() != true &&
                settingsDir.resolveSibling("fluxo-kmp-conf").exists())
    )
    if (useLocalHarness) {
        includeBuild("../fluxo-kmp-conf/self")
        includeBuild("../fluxo-kmp-conf")
    }
}

plugins {
    // https://plugins.gradle.org/plugin/com.gradle.develocity
    id("com.gradle.develocity") version "4.6.0"
}

dependencyResolutionManagement {
    // :kotlinNodeJsSetup requires adding of project repository
    // repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        // No snapshot repository on purpose: this build resolves only released
        // dependencies. `0.1.0-SNAPSHOT` is the version this project *publishes*
        // to the Central Portal snapshot repo, never one it consumes. (The retired
        // OSSRH snapshot repos that used to live here have 404'd since Sonatype's
        // shutdown; re-add a live repo only if a real -SNAPSHOT dependency appears.)
    }
}

rootProject.name = "fluxo"

include(":fluxo-common")
include(":fluxo-core")
include(":fluxo-data")

include(":benchmarks:jmh")
