package impl

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency


internal val Project.catalogs: VersionCatalogsExtension
    get() = extensions.getByType(VersionCatalogsExtension::class)

internal val Project.libsCatalog: VersionCatalog
    get() = catalogs.named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> {
    return findLibrary(alias).get()
}

internal fun VersionCatalog.plugin(alias: String): Provider<PluginDependency> {
    return findPlugin(alias).get()
}
