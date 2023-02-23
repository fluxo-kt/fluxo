@file:Suppress("TooManyFunctions")

package fluxo

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginExtension
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
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType


/*
 * Set up publishing. Useful resources:
 * - https://kotlinlang.org/docs/mpp-publish-lib.html
 * - https://central.sonatype.org/publish/publish-guide/
 * - https://central.sonatype.org/publish/publish-gradle/
 * - https://central.sonatype.org/publish/requirements/coordinates/#choose-your-coordinates
 * - https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/
 * - https://dev.to/kotlin/how-to-build-and-publish-a-kotlin-multiplatform-library-going-public-4a8k
 */

// TODO: Set RC/alpha/beta releases status to Ivy's: milestone/integration when it will be possible
//  https://github.com/gradle/gradle/issues/12600
//  https://github.com/gradle/gradle/issues/20016

private const val USE_DOKKA: Boolean = true


fun Project.setupPublication() {
    val config = requireDefaults<PublicationConfig>()
    when {
        hasExtension<KotlinMultiplatformExtension>() -> setupPublicationMultiplatform(config)
        hasExtension<LibraryExtension>() -> setupPublicationAndroidLibrary(config)
        hasExtension { GradlePluginDevelopmentExtension::class } -> setupPublicationGradlePlugin(config)
        hasExtension<KotlinJvmProjectExtension>() -> setupPublicationKotlinJvm(config)
        hasExtension { JavaPluginExtension::class } -> setupPublicationJava(config)
        else -> error("Unsupported project type for publication")
    }
}

private fun Project.setupPublicationMultiplatform(config: PublicationConfig) {
    applyMavenPublishPlugin()

    group = config.group
    version = config.version

    setupPublicationExtension(config)
    setupPublicationRepository(config)

    if (!isGenericCompilationEnabled) return
    multiplatformExtension.apply {
        if (targets.any { it.platformType == KotlinPlatformType.androidJvm }) {
            android {
                publishLibraryVariants("release", "debug")
            }

            // Gradle 8 compatibility
            if (config.isSigningEnabled) {
                val deps = tasks.matching {
                    it.name.startsWith("sign") && it.name.endsWith("Publication")
                }
                tasks.matching {
                    it.name.endsWith("PublicationToMavenLocal")
                        || it.name.endsWith("PublicationToMavenRepository")
                }.configureEach {
                    dependsOn(deps)
                }
            }
        }
    }
}

private fun Project.setupPublicationAndroidLibrary(config: PublicationConfig) {
    if (!isGenericCompilationEnabled) {
        return
    }

    applyMavenPublishPlugin()

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

private fun Project.setupPublicationGradlePlugin(config: PublicationConfig) {
    applyMavenPublishPlugin()

    group = config.group
    version = config.version

    val gradlePluginExtension = extensions.getByType<GradlePluginDevelopmentExtension>()

    val sourceJarTask by tasks.creating(Jar::class) {
        from(gradlePluginExtension.pluginSourceSet.java.srcDirs)
        archiveClassifier.set("sources")
    }

    afterEvaluate {
        setupPublicationExtension(config, sourceJarTask)
    }

    setupPublicationRepository(config)
}

private fun Project.setupPublicationKotlinJvm(config: PublicationConfig) {
    if (!isGenericCompilationEnabled) {
        return
    }

    applyMavenPublishPlugin()

    group = config.group
    version = config.version

    val kotlinPluginExtension = extensions.getByType<KotlinJvmProjectExtension>()

    val sourceJarTask by tasks.creating(Jar::class) {
        from(kotlinPluginExtension.sourceSets.getByName("main").kotlin.srcDirs)
        archiveClassifier.set("sources")
    }

    setupPublicationExtension(config, sourceJarTask)
    setupPublicationRepository(config)
}

private fun Project.setupPublicationJava(config: PublicationConfig) {
    if (!isGenericCompilationEnabled) {
        return
    }

    applyMavenPublishPlugin()

    group = config.group
    version = config.version

    val javaPluginExtension = extensions.getByType<JavaPluginExtension>()

    val sourceJarTask by tasks.creating(Jar::class) {
        from(javaPluginExtension.sourceSets.getByName("main").java.srcDirs)
        archiveClassifier.set("sources")
    }

    setupPublicationExtension(config, sourceJarTask)
    setupPublicationRepository(config)
}

internal fun MavenPublication.setupPublicationPom(project: Project, config: PublicationConfig) {
    // Publish docs with each artifact.
    val useDokka = USE_DOKKA && !config.isSnapshot
    try {
        check(useDokka) { "Dokka disabled" }

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
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        if (useDokka) {
            project.logger.lifecycle("Fallback to Javadoc. Dokka publication setup error: $e", e)
        }
        artifact(project.javadocJarTask())
    }

    pom {
        name.set(config.projectName)
        description.set(config.projectDescription)
        url.set(config.publicationUrl)

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
            config.scmTag.takeIf { !it.isNullOrEmpty() }?.let {
                tag.set(it)
            }
        }
    }
}

internal fun Project.setupPublicationRepository(config: PublicationConfig) {
    if (config.isSigningEnabled) {
        logger.lifecycle("SIGNING KEY SET, applying signing configuration")
        plugins.apply("signing")
    } else {
        logger.warn("SIGNING KEY IS NOT SET! Publications are unsigned")
    }

    extensions.configure<PublishingExtension> {
        if (config.isSigningEnabled) {
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

private fun Project.setupPublicationExtension(config: PublicationConfig, sourceJarTask: Jar? = null) {
    extensions.configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            sourceJarTask?.let { artifact(it) }
            setupPublicationPom(project, config)
        }
    }
}

private fun Project.applyMavenPublishPlugin() = plugins.apply("maven-publish")

private fun Project.javadocJarTask(): Task {
    val taskName = "javadocJar"
    return tasks.findByName(taskName) ?: tasks.create<Jar>(taskName).apply(Jar::setupJavadocJar)
}

private fun Jar.setupJavadocJar() {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Assembles a jar archive containing the Javadoc API documentation."
    archiveClassifier.set("javadoc")
}
