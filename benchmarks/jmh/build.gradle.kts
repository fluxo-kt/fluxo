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
    jmh(libs.jmh.core)
    jmh(libs.jmh.generator.annprocess)

    implementation(projects.fluxoCore)
    implementation(projects.fluxoData)


    // Libraries to compare/benchmark with

    // Ballast
    implementation("io.github.copper-leaf:ballast-core:" + libs.versions.ballast.get())

    // MVICore
    implementation("com.github.badoo.mvicore:mvicore:" + libs.versions.mvicore.get())
    implementation("com.github.badoo.mvicore:binder:" + libs.versions.mvicore.get())
    implementation(libs.kotlinx.coroutines.reactive)
    implementation(libs.rxjava2)

    // MVIKotlin
    implementation("com.arkivanov.mvikotlin:mvikotlin:" + libs.versions.mvikotlin.get())
    implementation("com.arkivanov.mvikotlin:mvikotlin-main:" + libs.versions.mvikotlin.get())
    implementation("com.arkivanov.mvikotlin:mvikotlin-extensions-coroutines:" + libs.versions.mvikotlin.get())

    // Orbit MVI
    implementation("org.orbit-mvi:orbit-core:" + libs.versions.orbit.get())
}

jmh {
    // https://github.com/melix/jmh-gradle-plugin#configuration-options

    // One! pattern (regular expression) for executed benchmarks
    includes.addAll(listOfNotNull(envOrPropValue("jmh")?.also {
        logger.lifecycle("JMH include='$it'")
    }))
    excludes.addAll(listOfNotNull(envOrPropValue("jmh_e")))

    // Warmup benchmarks to include in the run with already selected.
    warmupBenchmarks.addAll(envOrPropValue("jmh_wmb") ?: ".*Warmup")

    warmupIterations.set(envOrPropInt("jmh_wi") ?: 2)
    iterations.set(envOrPropInt("jmh_i") ?: 4)
    threads.set(envOrPropInt("jmh_t") ?: 4)
    fork.set(envOrPropInt("jmh_f") ?: 2)

    // Benchmark mode: [Throughput/thrpt, AverageTime/avgt, SampleTime/sample, SingleShotTime/ss, All/all]
    benchmarkMode.set(envOrPropList("jmh_bm").ifEmpty { listOf("thrpt", "avgt", "ss") })

    // Output time unit. Available time units are: [m, s, ms, us, ns].
    timeUnit.set(envOrPropValue("jmh_tu") ?: "ms")

    jmhVersion.set(libs.versions.jmh)

    jvmArgsAppend.add("-Dkotlinx.coroutines.debug=off")
}
