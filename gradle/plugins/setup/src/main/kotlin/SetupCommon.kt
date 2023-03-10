import impl.libsCatalog
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import kotlin.jvm.optionals.getOrNull

internal fun Project.setupKotlinJvmToolchain(kotlin: KotlinTopLevelExtension) {
    libsCatalog.findVersion("javaToolchain").getOrNull()?.let {
        kotlin.jvmToolchain(it.toString().substringAfter('.').toInt())
    }
}
