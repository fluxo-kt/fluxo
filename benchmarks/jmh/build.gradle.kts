@file:Suppress("DSL_SCOPE_VIOLATION")

// TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jmh)
}

dependencies {
    jmh(libs.jmh.core)
    jmh(libs.jmh.generator.annprocess)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)

    implementation(libs.kotlinx.coroutines.core)
    implementation(projects.fluxoCore)
    implementation(projects.fluxoData)


    // Libraries to compare/benchmark against

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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9.let {
            languageVersion.set(it)
            apiVersion.set(it)
        }

        // https://github.com/JetBrains/kotlin/blob/master/compiler/testData/cli/jvm/extraHelp.out
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-progressive",
        )

        useK2.set(true)
    }
}

jmh {
    // https://github.com/melix/jmh-gradle-plugin#configuration-options

    // One! pattern (regular expression) for benchmarks to be executed
    includes.addAll()
    excludes.addAll()

    // Warmup benchmarks to include in the run with already selected.
    warmupBenchmarks.addAll(".*Warmup")

    warmupIterations.set(2)
    iterations.set(3)
    fork.set(2)

    // Benchmark mode. Available modes are: [Throughput/thrpt, AverageTime/avgt, SampleTime/sample, SingleShotTime/ss, All/all]
    benchmarkMode.set(listOf("thrpt", "avgt", "ss"))
    // Output time unit. Available time units are: [m, s, ms, us, ns].
    timeUnit.set("ms")

    jmhVersion.set(libs.versions.jmh)
}
