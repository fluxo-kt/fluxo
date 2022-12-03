#!/usr/bin/env kotlin

@file:DependsOn("org.jsoup:jsoup:1.15.3")

import org.jsoup.parser.Parser
import java.io.File

// Kover summary
try {
    val koverFile = File("build/reports/kover-merged-report.xml")
    if (koverFile.exists()) {
        val dom = Parser.xmlParser().parseInput(koverFile.readText(), "")
        val counters = dom.select("report > counter").reversed()
        if (counters.isNotEmpty()) {
            println("#### Code metrics and coverage")
            println("| Tracked  | Total | Covered | Missed |")
            println("| -------  | ----- | ------- | ------ |")

            /**
            | First Header  | Second Header |
            | ------------- | ------------- |
            | Content Cell  | Content Cell  |

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
                    "| %s  | %d | %.2f%% (%d) | %.2f%% (%d) |".format(
                        type, total, covered / totalD * 100, covered, missed / totalD * 100, missed
                    )
                )
            }
        }
    }
} catch (e: Throwable) {
    e.printStackTrace(System.err)
}
