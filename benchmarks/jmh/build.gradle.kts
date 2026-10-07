plugins {
    alias(libs.plugins.jmh)
    // VisualFSM 4 generates its transition factories with KSP.
    alias(libs.plugins.kotlin.ksp)
}

fkcSetupKotlinApp(
    optIns = listOf("kt.fluxo.common.annotation.ExperimentalFluxoApi"),
) {
    kotlinLangVersion = "2.3"
    // Unpublished app: comparison libraries ship Java 21 bytecode (motorro CSM 4, VisualFSM 4), so the
    // benchmarks compile and run on the Gradle daemon's JDK, which every build environment already has.
    javaLangTarget = "25"

    setupCoroutines = true
    setupDependencies = true
    addStdlibDependency = true

    // Internal benchmark app — opt out of the root-inherited library publication.
    enablePublication = false
}

dependencies {
    // JMH, a JVM harness for building, running, and analysing nano/micro/milli/macro benchmarks
    jmh(libs.jmh.core)
    jmh(libs.jmh.generator.annprocess)

    implementation(libs.kotlinx.coroutines.core)


    implementation(projects.fluxoCore)


    // region Libraries to compare/benchmark with

    // Ballast
    implementation(libs.bench.ballast)

    // MVICore
    implementation(libs.bench.mvicore.core)
    implementation(libs.bench.mvicore.binder)
    implementation(libs.kotlinx.coroutines.reactive)
    implementation(libs.rxjava2)

    // MVIKotlin
    implementation(libs.bench.mvikotlin.core)
    implementation(libs.bench.mvikotlin.main)
    implementation(libs.bench.mvikotlin.coroutines)

    // Orbit MVI
    implementation(libs.bench.orbit)

    // Respawn FlowMVI
    implementation(libs.bench.flowmvi)

    // VisualFSM
    implementation(libs.bench.visualfsm.core)
    ksp(libs.bench.visualfsm.compiler)

    // Freeletics FlowRedux
    implementation(libs.bench.flowredux)

    // genaku Reduce
    implementation(libs.bench.genaku.reduce)
    implementation(enforcedPlatform(libs.kotlinx.coroutines.bom))

    // motorro CommonStateMachine
    implementation(libs.bench.motorro.core)
    implementation(libs.bench.motorro.coroutines)

    // Redux Kotlin
    implementation(libs.bench.reduxkotlin)

    // Reduktor
    implementation(libs.bench.reduktor.coroutines)
    implementation(libs.bench.reduktor.core)

    // Tinder StateMachine
    implementation(libs.bench.tinder.statemachine)

    // Mobius.kt
    implementation(libs.bench.mobiuskt.core)
    implementation(libs.bench.mobiuskt.coroutines)
    implementation(libs.bench.mobiuskt.extras)

    // Elmslie
    implementation(libs.bench.elmslie)

    // endregion
}

jmh {
    // https://github.com/melix/jmh-gradle-plugin#configuration-options

    // One! pattern (regular expression) for executed benchmarks
    var jmhStart = false
    includes.addAll(listOfNotNull(envOrPropValue("jmh")?.also {
        logger.lifecycle("JMH include='$it'")
        jmhStart = true
    }))
    excludes.addAll(listOfNotNull(envOrPropValue("jmh_e")?.also {
        if (jmhStart) logger.lifecycle("JMH exclude='$it'")
    }))

    // Warmup benchmarks to include in the run with already selected.
    warmupBenchmarks.addAll((envOrPropValue("jmh_wmb") ?: ".*Warmup").also {
        if (jmhStart) logger.lifecycle("JMH warmup='$it'")
    })


    warmupIterations.set((envOrPropInt("jmh_wi") ?: 1).also {
        if (jmhStart) logger.lifecycle("JMH warmupIterations='$it'")
    })
    iterations.set((envOrPropInt("jmh_i") ?: 4).also {
        if (jmhStart) logger.lifecycle("JMH iterations='$it'")
    })
    threads.set((envOrPropInt("jmh_t") ?: 2).let { threads ->
        val totalCpus = Runtime.getRuntime().availableProcessors()
        if (jmhStart) logger.lifecycle("JMH threads='$threads' (from $totalCpus possible)")
        threads.coerceIn(1, totalCpus)
    })
    fork.set((envOrPropInt("jmh_f") ?: 1).also {
        if (jmhStart) logger.lifecycle("JMH forks='$it'")
    })

    // Benchmark mode: [Throughput/thrpt, AverageTime/avgt, SampleTime/sample, SingleShotTime/ss, All/all]
    benchmarkMode.set(envOrPropList("jmh_bm").ifEmpty { listOf("thrpt", "avgt") }.also {
        if (jmhStart) logger.lifecycle("JMH benchmarkModes='$it'")
    })

    // Output time unit. Available time units are: [m, s, ms, us, ns].
    timeUnit.set((envOrPropValue("jmh_tu") ?: "ms").also {
        if (jmhStart) logger.lifecycle("JMH timeUnit='$it'")
    })

    // `.github/workflows/benchmark-summary.main.kts` parses the TEXT table.
    resultFormat.set("TEXT")

    jmhVersion.set(libs.versions.jmh)

    jvmArgsAppend.add("-Dkotlinx.coroutines.debug=off")
}

tasks.matching { it.name.startsWith("jmh") }.configureEach {
    notCompatibleWithConfigurationCache(
        "me.champeau.jmh generated JMH tasks capture Project state on Gradle 9.",
    )
}
