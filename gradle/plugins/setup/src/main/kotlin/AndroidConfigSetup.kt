import com.android.build.gradle.BaseExtension

class AndroidConfigSetup(
    val minSdkVersion: Int = 15,
    val compileSdkVersion: Int,
    val targetSdkVersion: Int = compileSdkVersion,
    val buildToolsVersion: String? = null,
    val configurator: (BaseExtension.() -> Unit)? = null,
)
