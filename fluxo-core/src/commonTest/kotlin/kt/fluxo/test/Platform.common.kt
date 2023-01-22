@file:Suppress("MatchingDeclarationName")

package kt.fluxo.test

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect val KMM_PLATFORM: Int

object Platform {
    const val JVM = 0
    const val JS = 1
    const val LINUX = 2
    const val MINGW = 3
    const val APPLE = 4

    val isNative
        get() = when (KMM_PLATFORM) {
            LINUX, MINGW, APPLE -> true
            else -> false
        }
}
