@file:Suppress("DSL_SCOPE_VIOLATION")

import fluxo.envOrPropInt
import fluxo.envOrPropList
import fluxo.envOrPropValue
import fluxo.setupJvmApp

// TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jmh)
}

setupJvmApp()

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

    // Kotlin Bloc
    implementation("com.1gravity:bloc-core-android:" + libs.versions.kotlinBloc.get())
    constraints {
        implementation("androidx.activity:activity:1.6.1")
        implementation("androidx.annotation:annotation-experimental:1.3.0")
        implementation("androidx.collection:collection:1.2.0")
        implementation("androidx.core:core:1.9.0")
        implementation("androidx.fragment:fragment:1.5.5")
        implementation("androidx.lifecycle:lifecycle-common-java8:2.5.1")
        implementation("androidx.lifecycle:lifecycle-common:2.5.1")
        implementation("androidx.lifecycle:lifecycle-livedata-core:2.5.1")
        implementation("androidx.lifecycle:lifecycle-livedata:2.5.1")
        implementation("androidx.lifecycle:lifecycle-process:2.5.1")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
        implementation("androidx.lifecycle:lifecycle-runtime:2.5.1")
        implementation("androidx.savedstate:savedstate:1.2.0")
        implementation("androidx.startup:startup-runtime:1.1.1")
        implementation(libs.essenty.instance.keeper)
        implementation(libs.essenty.lifecycle)
    }
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
            "-opt-in=kotlin.experimental.ExperimentalTypeInference",
            "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kt.fluxo.core.annotation.ExperimentalFluxoApi",
            "-progressive",
        )

        useK2.set(true)
    }
}

jmh {
    // https://github.com/melix/jmh-gradle-plugin#configuration-options

    // One! pattern (regular expression) for benchmarks to be executed
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

    // Benchmark mode. Available modes are: [Throughput/thrpt, AverageTime/avgt, SampleTime/sample, SingleShotTime/ss, All/all]
    benchmarkMode.set(envOrPropList("jmh_bm").ifEmpty { listOf("thrpt", "avgt", "ss") })

    // Output time unit. Available time units are: [m, s, ms, us, ns].
    timeUnit.set(envOrPropValue("jmh_tu") ?: "ms")

    jmhVersion.set(libs.versions.jmh)
}
