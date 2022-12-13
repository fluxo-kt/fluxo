package fluxo

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.existing
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension


/*
 * Set up publishing. Useful resources:
 * - https://kotlinlang.org/docs/mpp-publish-lib.html
 * - https://central.sonatype.org/publish/publish-guide/
 * - https://central.sonatype.org/publish/publish-gradle/
 * - https://central.sonatype.org/publish/requirements/coordinates/#choose-your-coordinates
 * - https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/
 * - https://dev.to/kotlin/how-to-build-and-publish-a-kotlin-multiplatform-library-going-public-4a8k
 */


fun Project.setupPublication() {
    val config = requireDefaults<PublicationConfig>()
    when {
        hasExtension<KotlinMultiplatformExtension>() -> setupPublicationMultiplatform(config)
        hasExtension<LibraryExtension>() -> setupPublicationAndroidLibrary(config)
        else -> error("Unsupported project type for publication")
    }
}

private fun Project.setupPublicationMultiplatform(config: PublicationConfig) {
    plugins.apply("maven-publish")

    group = config.group
    version = config.version

    extensions.configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            setupPublicationPom(project, config)
        }
    }

    setupPublicationRepository(config)

    if (Compilations.isGenericEnabled) {
        multiplatformExtension.apply {
            android {
                publishLibraryVariants("release", "debug")
            }
        }
    }
}

private fun Project.setupPublicationAndroidLibrary(config: PublicationConfig) {
    if (!Compilations.isGenericEnabled) {
        return
    }

    plugins.apply("maven-publish")

    val androidExtension = extensions.getByType<LibraryExtension>()

    val sourceJarTask by tasks.creating(Jar::class) {
        from(androidExtension.sourceSets.getByName("main").java.srcDirs)
        archiveClassifier.set("source")
    }

    fun PublicationContainer.createMavenPublication(name: String, artifactIdSuffix: String) {
        create<MavenPublication>(name) {
            from(components[name])
            artifact(sourceJarTask)

            groupId = config.group
            version = config.version
            artifactId = "${project.name}$artifactIdSuffix"

            setupPublicationPom(project, config)
        }
    }

    afterEvaluate {
        extensions.configure<PublishingExtension> {
            publications {
                createMavenPublication(name = "debug", artifactIdSuffix = "-debug")
                createMavenPublication(name = "release", artifactIdSuffix = "")
            }
        }
    }

    setupPublicationRepository(config)
}

internal fun MavenPublication.setupPublicationPom(
    project: Project,
    config: PublicationConfig,
) {
    // Publish docs with each artifact.
    try {
        val taskName = "dokkaHtmlAsJavadoc"
        val dokkaHtmlAsJavadoc = project.tasks.run {
            findByName(taskName) ?: run {
                val dokkaHtml by project.tasks.existing(DokkaTask::class)
                create<Jar>(taskName) {
                    setupJavadocJar()
                    from(dokkaHtml)
                }
            }
        }
        artifact(dokkaHtmlAsJavadoc)
    } catch (e: Throwable) {
        project.logger.lifecycle("Fallback to Javadoc. Dokka publication setup error: $e", e)
        artifact(project.javadocJarTask())
    }

    pom {
        name.set(config.projectName)
        description.set(config.projectDescription)
        url.set(config.projectUrl)

        if (!config.licenseName.isNullOrEmpty()) {
            licenses {
                license {
                    name.set(config.licenseName)
                    url.set(config.licenseUrl)
                }
            }
        }

        developers {
            developer {
                id.set(config.developerId)
                name.set(config.developerName)
                email.set(config.developerEmail)
            }
        }

        scm {
            url.set(config.projectUrl)
            connection.set(config.scmUrl)
            developerConnection.set(config.scmUrl)
        }
    }
}

internal fun Project.setupPublicationRepository(config: PublicationConfig) {
    val isSigningEnabled = !config.signingKey.isNullOrEmpty()

    if (isSigningEnabled) {
        plugins.apply("signing")
    }

    extensions.configure<PublishingExtension> {
        if (isSigningEnabled) {
            extensions.configure<SigningExtension> {
                useInMemoryPgpKeys(config.signingKey, config.signingPassword)
                sign(publications)
            }
        }

        repositories {
            maven {
                setUrl(config.repositoryUrl)

                credentials {
                    username = config.repositoryUserName
                    password = config.repositoryPassword
                }
            }
        }
    }
}

private fun Project.javadocJarTask(): Task {
    val taskName = "javadocJar"
    return tasks.findByName(taskName) ?: tasks.create<Jar>(taskName).apply(Jar::setupJavadocJar)
}

private fun Jar.setupJavadocJar() {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Assembles a jar archive containing the Javadoc API documentation."
    archiveClassifier.set("javadoc")
}
