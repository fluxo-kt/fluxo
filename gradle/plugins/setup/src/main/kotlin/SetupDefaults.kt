import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

fun Project.setupDefaults(
    multiplatformConfigurator: MultiplatformConfigurator? = null,
    androidConfig: AndroidConfigSetup? = null,
    kotlinConfig: KotlinConfigSetup? = null,
    publicationConfig: PublicationConfig? = null,
) {
    extra.set(
        DEFAULTS_KEY,
        listOfNotNull(
            multiplatformConfigurator,
            androidConfig,
            kotlinConfig,
            publicationConfig,
        )
    )
}

internal inline fun <reified T : Any> Project.requireDefaults(): T =
    requireNotNull(getDefaults()) { "Defaults not found for type ${T::class}" }

internal inline fun <reified T : Any> Project.getDefaults(): T? = getDefaults { it as? T }

private fun <T : Any> Project.getDefaults(mapper: (Any) -> T?): T? {
    return getDefaultsList()?.asSequence()?.mapNotNull(mapper)?.firstOrNull()
        ?: parent?.getDefaults(mapper)
}

@Suppress("UNCHECKED_CAST", "IdentifierGrammar", "CastToNullableType")
private fun Project.getDefaultsList(): MutableList<Any>? {
    return extra.takeIf { it.has(DEFAULTS_KEY) }?.get(DEFAULTS_KEY) as ArrayList<Any>?
}

private const val DEFAULTS_KEY = "setup.defaults"
