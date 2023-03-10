package impl

import groovy.time.TimeCategory
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import java.io.FileOutputStream
import java.util.Date
import java.util.concurrent.ConcurrentLinkedQueue
import javax.xml.stream.XMLOutputFactory

/**
 * Exports merged JUnit-like XML tests report for all tests in all projects.
 */
@DisableCachingByDefault(because = "Not cacheable")
internal abstract class TestsReportsMergeTask : DefaultTask() {

    @get:OutputFile
    abstract val output: RegularFileProperty

    private val testResults = ConcurrentLinkedQueue<ReportTestResult>()

    @TaskAction
    fun merge() {
        logger.info("Input")
        logger.info("{} tests", testResults.size)
        val outputFile = output.get().asFile.absoluteFile
        logger.info("Output = {}", outputFile)

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

            val mappedTestResults = testResults
                .groupBy { it.testSuite }
                .mapValues { it.value.sortedBy { t -> t.name } }

            for ((testSuite, results) in mappedTestResults) {
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
                    val testCountSum =
                        r.skippedTestCount + r.failedTestCount + r.successfulTestCount
                    testSuiteTests += maxOf(r.testCount, testCountSum)
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
                writer.writeAttribute("time", (testSuiteTimeMillis / MILLIS_IN_SECOND_F).toString())


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
                    val timeInSeconds = ((r.endTime - r.startTime) / MILLIS_IN_SECOND_F).toString()
                    writer.writeAttribute("time", timeInSeconds)

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
            val status = getStatusFrom(totalFailures, totalSuccesses)
            writer.writeStartElement("total")
            writer.writeAttribute("status", status)
            writer.writeAttribute("tests", totalTests.toString())
            writer.writeAttribute("skipped", totalSkipped.toString())
            writer.writeAttribute("failures", totalFailures.toString())
            writer.writeAttribute("timestamp", System.currentTimeMillis().toString())
            writer.writeAttribute("time", (totalTimeMillis / MILLIS_IN_SECOND_F).toString())
            writer.writeAttribute("kmpTargets", kmpTargets.size.toString())
            writer.writeEndElement()

            writer.writeEndElement()
            writer.writeEndDocument()
            writer.flush()
        } finally {
            writer.close()
        }

        // Final test results report in console
        val now = System.currentTimeMillis()
        val totalSuccesses = totalTests - totalFailures - totalSkipped
        val status = getStatusFrom(totalFailures, totalSuccesses)
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

    private fun getStatusFrom(totalFailures: Long, totalSuccesses: Long) = when {
        totalFailures > 0 -> "FAILED"
        totalSuccesses > 0 -> "SUCCESS"
        else -> "SKIPPED"
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
        var name = desc.displayName ?: ""
        if (!name.endsWith(']')) {
            val targetName = (task as? KotlinTest)?.targetName ?: testTaskName
            if (targetName.isNotBlank()) {
                name += "[$targetName]"
            }
        }

        kmpTarget = name.substringAfterLast('[', "").trimEnd(']').takeIf { it.isNotEmpty() }

        // Show target details in test name (browser/node, background, and so on.)
        if (kmpTarget != testTaskName && ", " !in name) {
            name = name.substringBeforeLast('[') + "[$testTaskName]"
        }

        this.name = name
    }
}

internal fun formatSummary(summary: String, fails: List<String>): String {
    val maxLength = summary.lines().maxOf { it.length + 1 }

    @Suppress("MagicNumber")
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

private const val MILLIS_IN_SECOND_F = 1000f
