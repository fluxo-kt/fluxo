package js.api

import kotlinx.validation.ApiValidationExtension
import org.gradle.api.Project

internal val Project.apiValidationExtensionOrNull: ApiValidationExtension?
    get() {
        val clazz: Class<*> = try {
            ApiValidationExtension::class.java
        } catch (_: Throwable) {
            return null
        }
        return generateSequence(this) { it.parent }
            .map { it.extensions.findByType(clazz) }
            .firstOrNull { it != null } as ApiValidationExtension?
    }

fun apiCheckEnabled(projectName: String, extension: ApiValidationExtension?): Boolean {
    return extension == null || projectName !in extension.ignoredProjects && !extension.validationDisabled
}
