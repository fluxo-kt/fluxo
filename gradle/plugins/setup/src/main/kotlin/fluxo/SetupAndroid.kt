package fluxo

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.gradle.internal.lint.AndroidLintTask
import com.android.build.gradle.internal.lint.AndroidLintTextOutputTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.setupAndroidLibrary() {
    setupAndroid(requireDefaults())
}

fun Project.setupAndroidApp(
    applicationId: String,
    versionCode: Int,
    versionName: String,
) {
    setupAndroid(requireDefaults())

    extensions.configure<BaseAppModuleExtension> {
        defaultConfig {
            this.applicationId = applicationId
            this.versionCode = versionCode
            this.versionName = versionName
        }
    }
}

private fun Project.setupAndroid(config: AndroidConfig) {
    setupAndroidCommon(config)

    tasks.withType<KotlinCompile> {
        enabled = project.isGenericCompilationEnabled
    }
}

internal fun Project.setupAndroidCommon(config: AndroidConfig) {
    extensions.configure<BaseExtension> {
        config.buildToolsVersion?.let {
            buildToolsVersion = it
        }

        compileSdkVersion(config.compileSdkVersion)

        defaultConfig {
            minSdk = config.minSdkVersion
            targetSdk = config.targetSdkVersion
        }

        val javaLangTarget = libsCatalog.findVersion("javaLangTarget").get().toString()
        compileOptions {
            val javaVersion = JavaVersion.toVersion(javaLangTarget)
            sourceCompatibility(javaVersion)
            targetCompatibility(javaVersion)
        }

        withGroovyBuilder {
            "kotlinOptions" {
                setProperty("jvmTarget", javaLangTarget)
            }
        }

        config.configurator?.invoke(this)
    }

    if (!project.isGenericCompilationEnabled || disableTests().get()) {
        tasks.withType<AndroidLintTask> { enabled = false }
        tasks.withType<AndroidLintTextOutputTask> { enabled = false }
    }
}
