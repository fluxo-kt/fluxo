package fluxo

class AndroidConfig(
    val minSdkVersion: Int = 15,
    val compileSdkVersion: Int,
    val targetSdkVersion: Int = compileSdkVersion,
    val buildToolsVersion: String? = null,
)
