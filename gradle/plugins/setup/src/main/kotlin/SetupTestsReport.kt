@file:Suppress("ArgumentListWrapping", "Wrapping")

import impl.TestsReportsMergeTask
import impl.checkIsRootProject
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.KotlinClosure2
import org.gradle.kotlin.dsl.withType

private const val TEST_REPORTS_TASK_NAME = "mergedTestReport"

fun Project.setupTestsReport() {
    checkIsRootProject()

    val mergedReport = tasks.register(TEST_REPORTS_TASK_NAME, TestsReportsMergeTask::class.java) {
        group = JavaBasePlugin.VERIFICATION_GROUP
        description = "Combines all tests reports from all modules to the published root one"
        output.set(project.layout.buildDirectory.file("tests-report-merged.xml"))
    }

    val disableTests by disableTests()
    if (disableTests) {
        logger.lifecycle("Test tasks disabled!")
    }

    allprojects {
        if (!disableTests) {
            val targetNames = hashSetOf(
                "check", "test", "allTests", "assemble", "build",
                "jvmTest", "jsTest", "jsNodeTest", "jsBrowserTest", "mingwX64Test",
            )
            tasks.matching { it.name in targetNames }.configureEach {
                finalizedBy(mergedReport)
            }
        }

        tasks.withType<AbstractTestTask> configuration@{
            if (disableTests || !isTestTaskAllowed()) {
                enabled = false
                return@configuration
            }

            val testTask = this
            finalizedBy(mergedReport)

            if (enabled) {
                mergedReport.configure {
                    mustRunAfter(testTask)
                }
            }

            testLogging {
                events = setOf(
                    TestLogEvent.FAILED,
                    TestLogEvent.SKIPPED,
                    TestLogEvent.STANDARD_OUT,
                    TestLogEvent.STANDARD_ERROR,
                )
                exceptionFormat = TestExceptionFormat.FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true
            }

            ignoreFailures = true // Always run all tests for all modules

            afterTest(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
                mergedReport.get().registerTestResult(testTask, desc, result)
            }))
        }
    }
}
