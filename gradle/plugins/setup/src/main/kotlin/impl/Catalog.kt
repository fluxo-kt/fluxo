package impl

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency
import kotlin.jvm.optionals.getOrNull


internal val Project.catalogs: VersionCatalogsExtension
    get() = extensions.getByType(VersionCatalogsExtension::class)

internal val Project.libsCatalog: VersionCatalog
    get() = catalogs.named("libs")


internal fun VersionCatalog.onLibrary(alias: String, body: (Provider<MinimalExternalModuleDependency>) -> Unit): Boolean {
    val opt = findLibrary(alias)
    if (opt.isPresent) {
        body(opt.get())
        return true
    }
    return false
}

internal fun VersionCatalog.d(alias: String): Provider<MinimalExternalModuleDependency> {
    return findLibrary(alias).get()
}


internal fun VersionCatalog.onBundle(
    alias: String,
    body: (Provider<ExternalModuleDependencyBundle>) -> Unit,
): Boolean {
    val opt = findBundle(alias)
    if (opt.isPresent) {
        body(opt.get())
        return true
    }
    return false
}

internal fun VersionCatalog.b(alias: String): Provider<ExternalModuleDependencyBundle> {
    return findBundle(alias).get()
}


internal fun VersionCatalog.plugin(alias: String): Provider<PluginDependency> {
    return findPlugin(alias).get()
}

internal fun VersionCatalog.onPlugin(alias: String, body: (PluginDependency) -> Unit): Boolean {
    val opt = findPlugin(alias)
    if (opt.isPresent) {
        body(opt.get().get())
        return true
    }
    return false
}


internal fun VersionCatalog.onVersion(alias: String, body: (String) -> Unit): Boolean {
    val opt = findVersion(alias)
    if (opt.isPresent) {
        body(opt.get().toString())
        return true
    }
    return false
}

internal fun VersionCatalog.v(alias: String): String {
    return findVersion(alias).get().toString()
}

internal fun VersionCatalog.optionalVersion(alias: String): String? {
    return findVersion(alias).getOrNull()?.toString()
}
