import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project

@Suppress("LongParameterList")
class AndroidConfigSetup(
    val compileSdkVersion: Int,
    val minSdkVersion: Int = 15,
    val targetSdkVersion: Int = compileSdkVersion,
    val lastSdkVersion: Int = targetSdkVersion,
    val buildToolsVersion: String? = null,
    val previewSdkVersion: String? = null,

    /** `true`/`false` for all modules, `null` to leave customizable for each module  */
    val enableBuildConfig: Boolean? = null,

    val setupCompose: Boolean = false,

    /**
     * Specifies a list of alternative languages to keep.
     *
     * @see com.android.build.api.dsl.BaseFlavor.resourceConfigurations
     */
    val languages: List<String> = listOf("en"),

    val configurator: (CommonExtension<*, *, *, *>.(Project) -> Unit)? = null,
)
