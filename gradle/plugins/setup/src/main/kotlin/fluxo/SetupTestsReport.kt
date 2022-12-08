package fluxo

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
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
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

@DisableCachingByDefault(because = "Not cacheable")
private abstract class TestsReportsMergeTask : DefaultTask() {

    @get:OutputFile
    abstract val output: RegularFileProperty

    private val resultsMap = ConcurrentHashMap<AbstractTestTask, Queue<Pair<TestDescriptor, TestResult>>>()

    @TaskAction
    fun merge() {
        logger.info("Input")
        logger.info("${resultsMap.size} test tasks with ${resultsMap.values.sumOf { it.size }} tests")
        val outputFile = output.get().asFile.absoluteFile
        logger.info("Output = $outputFile")

        val factory = XMLOutputFactory.newInstance()
        val writer = factory.createXMLStreamWriter(FileOutputStream(outputFile))
        try {
            writer.writeStartDocument()
            writer.writeStartElement("report")

            // FIXME: Group tests by class instead of tasks (so platform tasks will be groupped in the)
            for ((testTask, queue) in resultsMap) {
                val projectName = testTask.project.name
                val targetName = (testTask as? KotlinTest)?.targetName ?: testTask.name

                for ((testSuite, results) in queue.groupBy {
                    val className = it.first.className ?: ""
                    ":$projectName $className"
                }) {
                    var tests = 0L
                    var skipped = 0L
                    var failures = 0L
                    var time = 0L
                    var timestamp = 0L
                    for ((_, r) in results) {
                        skipped += r.skippedTestCount
                        failures += r.failedTestCount
                        tests += maxOf(r.testCount, r.skippedTestCount + r.failedTestCount + r.successfulTestCount)
                        time += r.endTime - r.startTime
                        if (r.endTime > timestamp) {
                            timestamp = r.endTime
                        }
                    }

                    writer.writeStartElement("testsuite")
                    writer.writeAttribute("name", testSuite)
                    writer.writeAttribute("tests", tests.toString())
                    writer.writeAttribute("skipped", skipped.toString())
                    writer.writeAttribute("failures", failures.toString())
                    if (VERBOSE_OUTPUT) {
                        writer.writeAttribute("errors", "0")
                        writer.writeAttribute("hostname", "")
                    }
                    writer.writeAttribute("timestamp", timestamp.toString())
                    writer.writeAttribute("time", (time / 1000f).toString())

                    if (VERBOSE_OUTPUT) {
                        writer.writeEmptyElement("properties")
                    }

                    for ((d, r) in results) {
                        writer.writeStartElement("testcase")

                        var name = d.name
                        if (targetName.isNotBlank()) {
                            name += "[$targetName]"
                        }

                        writer.writeAttribute("name", name)
                        writer.writeAttribute("classname", d.className ?: "")
                        writer.writeAttribute("time", ((r.endTime - r.startTime) / 1000f).toString())

                        if (r.resultType == TestResult.ResultType.SKIPPED) {
                            writer.writeEmptyElement("skipped")
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
            }

            writer.writeEndElement()
            writer.writeEndDocument()
            writer.flush()
        } finally {
            writer.close()
        }

        // TODO: Final test results report in console

        logger.lifecycle("Merged XML tests report to $outputFile")
        resultsMap.clear()
    }

    fun registerTestResult(testTask: AbstractTestTask, desc: TestDescriptor, result: TestResult) {
        var queue = resultsMap[testTask]
        if (queue == null) {
            val q = ConcurrentLinkedQueue<Pair<TestDescriptor, TestResult>>()
            queue = resultsMap.putIfAbsent(testTask, q) ?: q
        }
        queue.add(desc to result)
    }

    private companion object {
        const val VERBOSE_OUTPUT = false
    }
}
