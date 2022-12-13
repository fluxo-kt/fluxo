package fluxo

class PublicationConfig(
    val group: String,
    val version: String,
    val projectName: String,
    val projectDescription: String,
    val projectUrl: String,
    val scmUrl: String,
    val licenseName: String?,
    val licenseUrl: String?,
    val developerId: String,
    val developerName: String,
    val developerEmail: String,
    val signingKey: String?,
    val signingPassword: String?,
    val repositoryUrl: String,
    val repositoryUserName: String?,
    val repositoryPassword: String?,
)
