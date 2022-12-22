package fluxo

@Suppress("LongParameterList", "MemberVisibilityCanBePrivate")
class PublicationConfig(
    val group: String,
    val version: String,
    val projectName: String,

    val projectDescription: String,
    val projectUrl: String,
    val scmUrl: String,

    val developerId: String,
    val developerName: String,
    val developerEmail: String,

    val signingKey: String?,
    val signingPassword: String?,

    val repositoryUserName: String?,
    val repositoryPassword: String?,

    val publicationUrl: String = projectUrl,
    val isSnapshot: Boolean = version.endsWith("SNAPSHOT", ignoreCase = true),
    val scmTag: String? = if (isSnapshot) "main" else "v$version",

    val repositoryUrl: String = when {
        isSnapshot -> "https://s01.oss.sonatype.org/content/repositories/snapshots/"
        else -> "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
    },

    val licenseName: String? = "The Apache License, Version 2.0",
    val licenseUrl: String? = "http://www.apache.org/licenses/LICENSE-2.0.txt",

    // Halpful links:
    // https://proandroiddev.com/publishing-android-libraries-to-mavencentral-in-2021-8ac9975c3e52
    // https://motorro.medium.com/thanks-a-lot-for-this-step-by-step-instructions-f6fecbe5a4e6
    // https://central.sonatype.org/publish/requirements/gpg/
)
