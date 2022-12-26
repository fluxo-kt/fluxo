@file:Suppress("DSL_SCOPE_VIOLATION")

// TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jmh)
}

dependencies {
    jmh(libs.jmh.core)
    jmh(libs.jmh.generator.annprocess)

    jmh(libs.kotlinx.coroutines.core)
    jmh(projects.fluxoCore)
    jmh(projects.fluxoData)
}

jmh {
    // https://github.com/melix/jmh-gradle-plugin#configuration-options

    warmupIterations.set(2)
    iterations.set(3)
    fork.set(2)

    // Benchmark mode. Available modes are: [Throughput/thrpt, AverageTime/avgt, SampleTime/sample, SingleShotTime/ss, All/all]
    benchmarkMode.set(listOf("thrpt", "avgt", "ss"))
    // Output time unit. Available time units are: [m, s, ms, us, ns].
    timeUnit.set("ms")

    jmhVersion.set(libs.versions.jmh)
}
