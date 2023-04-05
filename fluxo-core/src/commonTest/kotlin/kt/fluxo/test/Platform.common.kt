@file:Suppress("MatchingDeclarationName")

package kt.fluxo.test

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect val KMM_PLATFORM: String

object Platform {
    const val JVM = "JVM"
    const val JS = "JS"
    const val LINUX = "LINUX"
    const val MINGW = "MINGW"
    const val APPLE = "APPLE"
}
