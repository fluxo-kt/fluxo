package fluxo

import groovy.time.TimeCategory
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.KotlinClosure2
import org.gradle.kotlin.dsl.withType
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import java.io.FileOutputStream
import java.lang.System.currentTimeMillis
import java.util.Date
import java.util.concurrent.ConcurrentLinkedQueue
import javax.xml.stream.XMLOutputFactory

fun Project.setupTestsReport() {
    checkIsRootProject()

    val mergedReport = tasks.register(TEST_REPORTS_TASK_NAME, TestsReportsMergeTask::class.java) {
        group = JavaBasePlugin.VERIFICATION_GROUP
        description = "Combines all tests reports from all modules to the published root one"
        output.set(project.layout.buildDirectory.file("tests-report-merged.xml"))
    }

    allprojects {
        tasks.matching { it.name == "check" || it.name == "allTests" }.configureEach {
            finalizedBy(mergedReport)
        }

        tasks.withType<AbstractTestTask> {
            val testTask = this
            finalizedBy(mergedReport)

            if (enabled) {
                mergedReport.configure {
                    mustRunAfter(testTask)
                }
            }

            testLogging {
                events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
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

private const val TEST_REPORTS_TASK_NAME = "mergedTestReport"

/**
 * Exports merged JUnit-like XML tests report for all tests in all projects.
 */
@DisableCachingByDefault(because = "Not cacheable")
private abstract class TestsReportsMergeTask : DefaultTask() {

    @get:OutputFile
    abstract val output: RegularFileProperty

    private val testResults = ConcurrentLinkedQueue<ReportTestResult>()

    @TaskAction
    fun merge() {
        logger.info("Input")
        logger.info("${testResults.size} tests")
        val outputFile = output.get().asFile.absoluteFile
        logger.info("Output = $outputFile")

        var totalTests = 0L
        var totalSkipped = 0L
        var totalFailures = 0L
        var totalTimeMillis = 0L

        val fails = ArrayList<String>()
        val kmpTargets = HashSet<String>()
        val factory = XMLOutputFactory.newInstance()
        val writer = factory.createXMLStreamWriter(FileOutputStream(outputFile))
        try {
            writer.writeStartDocument()
            writer.writeStartElement("report")

            for ((testSuite, results) in testResults
                .groupBy { it.testSuite }
                .mapValues { it.value.sortedBy { t -> t.name } }
            ) {
                writer.writeStartElement("testsuite")
                writer.writeAttribute("name", testSuite)

                var testSuiteTests = 0L
                var testSuiteSkipped = 0L
                var testSuiteFailures = 0L
                var testSuiteTimeMillis = 0L
                var testSuiteTimestamp = 0L
                for (i in results.indices) {
                    val r = results[i].result
                    testSuiteSkipped += r.skippedTestCount
                    testSuiteFailures += r.failedTestCount
                    testSuiteTests += maxOf(r.testCount, r.skippedTestCount + r.failedTestCount + r.successfulTestCount)
                    testSuiteTimeMillis += r.endTime - r.startTime
                    if (r.endTime > testSuiteTimestamp) {
                        testSuiteTimestamp = r.endTime
                    }
                }
                totalTests += testSuiteTests
                totalSkipped += testSuiteSkipped
                totalFailures += testSuiteFailures
                totalTimeMillis += testSuiteTimeMillis

                writer.writeAttribute("tests", testSuiteTests.toString())
                writer.writeAttribute("skipped", testSuiteSkipped.toString())
                writer.writeAttribute("failures", testSuiteFailures.toString())
                if (VERBOSE_OUTPUT) {
                    writer.writeAttribute("errors", "0")
                    writer.writeAttribute("hostname", "")
                }
                writer.writeAttribute("timestamp", testSuiteTimestamp.toString())
                writer.writeAttribute("time", (testSuiteTimeMillis / 1000f).toString())


                if (VERBOSE_OUTPUT) {
                    writer.writeEmptyElement("properties")
                }

                for (i in results.indices) {
                    val rtr = results[i]
                    val r = rtr.result
                    writer.writeStartElement("testcase")

                    rtr.kmpTarget?.let { kmpTargets += it }

                    writer.writeAttribute("name", rtr.name)
                    writer.writeAttribute("classname", rtr.className)
                    writer.writeAttribute("time", ((r.endTime - r.startTime) / 1000f).toString())

                    when (r.resultType) {
                        TestResult.ResultType.SKIPPED -> {
                            writer.writeEmptyElement("skipped")
                        }

                        TestResult.ResultType.FAILURE -> {
                            writer.writeStartElement("failure")

                            val es = r.exceptions
                            writer.writeAttribute("message", es.firstOrNull()?.toString() ?: "")
                            writer.writeAttribute("type", es.firstOrNull()?.javaClass?.name ?: "")
                            writer.writeCData(es.joinToString("\n\n") { it.stackTraceToString() })

                            fails += "$testSuite.${rtr.name}"

                            writer.writeEndElement()
                        }

                        else -> {}
                    }

                    writer.writeEndElement()
                }

                if (VERBOSE_OUTPUT) {
                    writer.writeStartElement("system-out")
                    writer.writeCData("")
                    writer.writeEndElement()

                    writer.writeStartElement("system-err")
                    writer.writeCData("")
                    writer.writeEndElement()
                }

                writer.writeEndElement()
            }

            val totalSuccesses = totalTests - totalFailures - totalSkipped
            val status = if (totalFailures > 0) "FAILED" else if (totalSuccesses > 0) "SUCCESS" else "SKIPPED"
            writer.writeStartElement("total")
            writer.writeAttribute("status", status)
            writer.writeAttribute("tests", totalTests.toString())
            writer.writeAttribute("skipped", totalSkipped.toString())
            writer.writeAttribute("failures", totalFailures.toString())
            writer.writeAttribute("timestamp", currentTimeMillis().toString())
            writer.writeAttribute("time", (totalTimeMillis / 1000f).toString())
            writer.writeAttribute("kmpTargets", kmpTargets.size.toString())
            writer.writeEndElement()

            writer.writeEndElement()
            writer.writeEndDocument()
            writer.flush()
        } finally {
            writer.close()
        }

        // Final test results report in console
        val now = currentTimeMillis()
        val totalSuccesses = totalTests - totalFailures - totalSkipped
        val status = if (totalFailures > 0) "FAILED" else if (totalSuccesses > 0) "SUCCESS" else "SKIPPED"
        val summary = "Overall tests result: $status (" +
            "$totalTests tests, " +
            "$totalSuccesses successes, " +
            "$totalFailures failures, " +
            "$totalSkipped skipped, " +
            "${kmpTargets.size} KMP targets" +
            ") " +
            "in ${TimeCategory.minus(Date(now), Date(now - totalTimeMillis))}" +
            "\n" +
            "Merged XML tests report to $outputFile"

        logger.lifecycle(formatSummary(summary, fails))
        testResults.clear()
    }

    fun registerTestResult(testTask: AbstractTestTask, desc: TestDescriptor, result: TestResult) {
        testResults.add(ReportTestResult(testTask, desc, result))
    }

    private companion object {
        const val VERBOSE_OUTPUT = false
    }
}

private class ReportTestResult(
    task: AbstractTestTask,
    desc: TestDescriptor,
    val result: TestResult,
) {
    val className = desc.className ?: ""
    val testSuite = ":${task.project.name} $className"
    val testTaskName = task.name.substringBeforeLast("Test")
    val kmpTarget: String?
    val name: String

    init {
        var name = desc.displayName
        if (!name.endsWith(']')) {
            val targetName = (task as? KotlinTest)?.targetName ?: testTaskName
            if (targetName.isNotBlank()) {
                name += "[$targetName]"
            }
        }

        kmpTarget = name.substringAfterLast('[', "").trimEnd(']').takeIf { it.isNotEmpty() }

        // Show target details in test name (browser/node, background, etc.)
        if (kmpTarget != testTaskName && ", " !in name) {
            name = name.substringBeforeLast('[') + "[$testTaskName]"
        }

        this.name = name
    }
}

private fun formatSummary(summary: String, fails: List<String>): String {
    val maxLength = summary.lines().maxOf { it.length + 1 }

    val sb = StringBuilder(maxLength * (6 + fails.size))
    for (i in 1..(maxLength + 2)) sb.append('_')
    sb.append('\n')

    summary.lines().joinTo(sb, separator = "\n", postfix = "\n") {
        "| " + it + " ".repeat(maxLength - it.length) + '|'
    }

    for (i in 1..(maxLength + 2)) sb.append('-')

    if (fails.isNotEmpty()) {
        sb.append('\n')
        for (fail in fails) {
            sb.append("| FAILED ").append(fail).append('\n')
        }
        for (i in 1..(maxLength + 2)) sb.append('-')
    }

    return sb.toString()
}
