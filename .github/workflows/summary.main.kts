#!/usr/bin/env kotlin

@file:DependsOn("org.jsoup:jsoup:1.15.3")

import org.jsoup.parser.Parser
import java.io.File

// Kover summary
try {
    val koverFile = File("build/reports/kover-merged-report.xml")
    if (koverFile.exists()) {
        System.err.println("Kover report FOUND: $koverFile")

        val dom = Parser.xmlParser().parseInput(koverFile.readText(), "")
        val counters = dom.select("report > counter").reversed()
        if (counters.isNotEmpty()) {
            println("#### Code metrics and coverage")
            println("| Tracked | Total | Covered | Missed |")
            println("| ------- | ----- | ------- | ------ |")

            /**
            | First Header | Second Header |
            | ------------ | ------------- |
            | Content Cell | Content Cell  |

            <details><summary>Title</summary>
            <p>Content</p>
            </details>
             */
            for (counter in counters) {
                val type = counter.attr("type").let {
                    when (it) {
                        "CLASS" -> "Classes"
                        "METHOD" -> "Methods"
                        "LINE" -> "Lines"
                        "BRANCH" -> "Branches"
                        "INSTRUCTION" -> "Instructions"
                        else -> it
                    }
                }
                val missed = counter.attr("missed").toIntOrNull() ?: 0
                val covered = counter.attr("covered").toIntOrNull() ?: 0
                val total = missed + covered
                val totalD = total.toDouble()

                println(
                    "| %s | %d | %.2f%% (%d) | %.2f%% (%d) |".format(
                        type, total, covered / totalD * 100, covered, missed / totalD * 100, missed
                    )
                )
            }
        } else {
            System.err.println("No <counter> tags found in report")
        }
    } else {
        System.err.println("Kover report NOT found: $koverFile")
    }
} catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
    System.err.println("Kover report error: $e")
    @Suppress("PrintStackTrace")
    e.printStackTrace(System.err)
}

// Tests summary
try {
    println()
    val testsFile = File("build/tests-report-merged.xml")
    if (testsFile.exists()) {
        System.err.println("Tests report FOUND: $testsFile")

        val dom = Parser.xmlParser().parseInput(testsFile.readText(), "")
        println("#### Tests summary")

        val total = dom.selectFirst("report > total")
        if (total != null) {
            println("| Result  | Total | Skipped | Failed | Duration (sec) | KMP targets |")
            println("| -------  | ----- | ------- | ------ | ------ | ------ |")

            val status = total.attr("status").let {
                when (it) {
                    "FAILED" -> "&#10060; FAILED" // ❌
                    "SUCCESS" -> "&#9989; SUCCESS" // ✅
                    "LINE" -> "SKIPPED"
                    else -> it
                }
            }
            val totalTests = total.attr("tests").toIntOrNull() ?: 0
            val skipped = total.attr("skipped").toIntOrNull() ?: 0
            val failures = total.attr("failures").toIntOrNull() ?: 0
            val kmpTargets = total.attr("kmpTargets").toIntOrNull() ?: 0
            val timeSeconds = total.attr("time") ?: "-"

            println("| %s | %d | %d | %d | %s | %d |".format(status, totalTests, skipped, failures, timeSeconds, kmpTargets))
        } else {
            System.err.println("No <total> tags found in tests report")
        }

        val failures = dom.select("testcase > failure")
        for (fail in failures) {
            val testcase = fail.parent()
            val testsuite = testcase?.parent()

            val suiteName = testsuite?.attr("name")?.takeIf { it.isNotEmpty() }
                ?: testcase?.attr("classname") ?: ""

            val testName = testcase?.attr("name")?.takeIf { it.isNotEmpty() }
                ?: fail.attr("type")

            val failDetails = fail.wholeText().trim().takeIf { it.isNotEmpty() } ?: fail.attr("message")

            // ❌
            println("<details><summary>&#10060; <i>$suiteName</i>.<b>$testName</b></summary><p><pre language=\"kotlin\">")
            println("$failDetails</pre></p></details>")
        }
    } else {
        System.err.println("Tests report NOT found: $testsFile")
    }
} catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
    System.err.println("Tests report error: $e")
    @Suppress("PrintStackTrace")
    e.printStackTrace(System.err)
}
