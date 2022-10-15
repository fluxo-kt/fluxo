import org.gradle.api.Project
import org.gradle.api.provider.Provider


private fun String?.tryAsBoolean() =
    arrayOf("true", "1", "on").any { it.equals(this, ignoreCase = true) }

fun Project.booleanProperty(name: String): Provider<Boolean> {
    val provider = stringProperty(name)
    return provider {
        provider.orNull.tryAsBoolean()
    }
}

private fun Project.stringProperty(name: String): Provider<String> {
    return provider {
        var value = findProperty(name)?.toString()
        if (value.isNullOrEmpty() && '.' !in name) {
            value = findProperty("org.gradle.project.$name")?.toString()
        }
        value
    }
}


private fun Project.envOrPropFlag(name: String): Provider<Boolean> {
    val env = providers.environmentVariable(name)
    val prop = stringProperty(name)
    return provider {
        var value: String? = env.orNull
        if (value.isNullOrEmpty()) {
            value = prop.orNull
        }
        value.tryAsBoolean()
    }
}

fun Project.isCI(): Provider<Boolean> = envOrPropFlag("CI")
fun Project.isRelease(): Provider<Boolean> = envOrPropFlag("RELEASE")


operator fun Provider<Boolean>.getValue(thisRef: Any?, property: Any?): Boolean = orNull == true
