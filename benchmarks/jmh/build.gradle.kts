@file:Suppress("TrailingCommaOnCallSite")

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jmh)
}

setupKotlin(
    config = KotlinConfigSetup(
        kotlinLangVersion = "latest",
        javaLangTarget = "11",
        optInInternal = true,
        optIns = listOf("kt.fluxo.core.annotation.ExperimentalFluxoApi"),
    )
)

dependencies {
    // JMH, a JVM harness for building, running, and analysing nano/micro/milli/macro benchmarks
    jmh(libs.jmh.core)
    jmh(libs.jmh.generator.annprocess)


    // Use the latest snapshot for local development to dogfood the lib.
    val isCI by isCI()
    if (isCI) {
        implementation(projects.fluxoCore)
    } else {
        implementation("io.github.fluxo-kt:fluxo-core:" + libs.versions.fluxoSnapshot.get())
    }


    // region Libraries to compare/benchmark with

    // Ballast
    implementation("io.github.copper-leaf:ballast-core:" + libs.versions.ballast.get())

    // MVICore
    val mviCoreVersion = libs.versions.mvicore.get()
    implementation("com.github.badoo.mvicore:mvicore:$mviCoreVersion")
    implementation("com.github.badoo.mvicore:binder:$mviCoreVersion")
    implementation(libs.kotlinx.coroutines.reactive)
    implementation(libs.rxjava2)

    // MVIKotlin
    val mviKotlinVersion = libs.versions.mvikotlin.get()
    implementation("com.arkivanov.mvikotlin:mvikotlin:$mviKotlinVersion")
    implementation("com.arkivanov.mvikotlin:mvikotlin-main:$mviKotlinVersion")
    implementation("com.arkivanov.mvikotlin:mvikotlin-extensions-coroutines:$mviKotlinVersion")

    // Orbit MVI
    implementation("org.orbit-mvi:orbit-core:" + libs.versions.orbit.get())

    // Respawn FlowMVI
    implementation("pro.respawn.flowmvi:core:" + libs.versions.respawnFlowMVI.get())

    // Respawn FlowMVI
    implementation("ru.kontur.mobile.visualfsm:visualfsm-core:" + libs.versions.visualfsm.get())

    // Freeletics FlowRedux
    implementation("com.freeletics.flowredux:flowredux:" + libs.versions.flowredux.get())

    // genaku Reduce
    implementation("com.github.genaku.reduce:reduce-core:" + libs.versions.genakuReduce.get())
    implementation(enforcedPlatform(libs.kotlinx.coroutines.bom))

    // motorro CommonStateMachine
    val motorroCsmVersion = libs.versions.motorroCommonStateMachine.get()
    implementation("com.motorro.commonstatemachine:commonstatemachine:$motorroCsmVersion")
    implementation("com.motorro.commonstatemachine:coroutines:$motorroCsmVersion")

    // Redux Kotlin
    implementation("org.reduxkotlin:redux-kotlin-threadsafe:" + libs.versions.reduxkotlin.get())

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

    jmhVersion.set(libs.versions.jmh)

    jvmArgsAppend.add("-Dkotlinx.coroutines.debug=off")
}
