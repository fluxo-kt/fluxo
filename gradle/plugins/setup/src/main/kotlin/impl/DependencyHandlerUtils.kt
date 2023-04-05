@file:Suppress("TooManyFunctions")

package impl

import org.gradle.api.Action
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.accessors.runtime.addConfiguredDependencyTo

internal fun DependencyHandler.ksp(dependencyNotation: Any) =
    add("ksp", dependencyNotation)


internal fun DependencyHandler.implementation(dependencyNotation: Any) =
    add("implementation", dependencyNotation)

internal fun DependencyHandler.implementation(
    dependencyNotation: Provider<*>,
    configuration: Action<ExternalModuleDependency>,
) = addConfiguredDependencyTo(this, "implementation", dependencyNotation, configuration)


internal fun DependencyHandler.testImplementation(dependencyNotation: Any) =
    add("testImplementation", dependencyNotation)


internal fun DependencyHandler.androidTestImplementation(dependencyNotation: Any) =
    add("androidTestImplementation", dependencyNotation)

internal fun DependencyHandler.androidTestImplementation(
    dependencyNotation: Provider<*>,
    configuration: Action<ExternalModuleDependency>,
) = addConfiguredDependencyTo(this, "androidTestImplementation", dependencyNotation, configuration)


internal fun DependencyHandler.debugImplementation(dependencyNotation: Any) =
    add("debugImplementation", dependencyNotation)

internal fun DependencyHandler.debugCompileOnly(dependencyNotation: Any) =
    add("debugCompileOnly", dependencyNotation)


internal fun DependencyHandler.runtimeOnly(dependencyNotation: Any) =
    add("runtimeOnly", dependencyNotation)


internal fun DependencyHandler.compileOnly(dependencyNotation: Any) =
    add("compileOnly", dependencyNotation)

internal fun DependencyHandler.compileOnlyWithConstraint(dependencyNotation: Any) {
    compileOnly(dependencyNotation)
    constraints.implementation(dependencyNotation)
}

internal fun DependencyConstraintHandler.implementation(constraintNotation: Any) =
    add("implementation", constraintNotation)
