package fluxo

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

fun Project.setupDefaults(
    multiplatformConfigurator: MultiplatformConfigurator? = null,
    androidConfig: AndroidConfig? = null,
    publicationConfig: PublicationConfig? = null,
) {
    extra.set(
        DEFAULTS_KEY,
        listOfNotNull(
            multiplatformConfigurator,
            androidConfig,
            publicationConfig,
        )
    )
}

internal inline fun <reified T : Any> Project.requireDefaults(): T =
    requireNotNull(getDefaults()) { "Defaults not found for type ${T::class}" }

internal inline fun <reified T : Any> Project.getDefaults(): T? =
    getDefaults { it as? T }

private fun <T : Any> Project.getDefaults(mapper: (Any) -> T?): T? =
    getDefaultsList()?.asSequence()?.mapNotNull(mapper)?.firstOrNull()
        ?: parent?.getDefaults(mapper)

@Suppress("UNCHECKED_CAST")
private fun Project.getDefaultsList(): MutableList<Any>? =
    extra.takeIf { it.has(DEFAULTS_KEY) }?.get(DEFAULTS_KEY) as ArrayList<Any>?

private const val DEFAULTS_KEY = "fluxo.defaults"
