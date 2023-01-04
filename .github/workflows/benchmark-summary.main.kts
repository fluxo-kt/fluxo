#!/usr/bin/env kotlin

import java.io.File
import java.math.BigDecimal
import java.util.Locale
import java.util.regex.Pattern

// JMH results summary
try {
    val koverFile = File("benchmarks/jmh/build/results/jmh/results.txt")
    if (koverFile.exists()) {
        System.err.println("JMH results FOUND: $koverFile")

        val text = koverFile.readText()

        data class JmhResult(
            val clazz: String,
            val name: String,
            val mode: String,
            val cnt: Int,
            val score: BigDecimal,
            val error: BigDecimal?,
            val units: String,
        ) {
            val asRawArray
                get() = arrayOf(name, mode, cnt, score, error?.let { "± $it" } ?: "", units)
        }

        val splitRegex = Pattern.compile("(?i)(?<![±�])\\s+", Pattern.UNICODE_CHARACTER_CLASS or Pattern.UNICODE_CASE).toRegex()
        var total = 0
        val resultsByClass = text.lineSequence().drop(1).mapNotNull l@{ line ->
            val l = line.trim()
            if (l.isEmpty()) {
                return@l null
            }

            total++
            val parts = l.split(splitRegex, 6)

            var i = 0
            val benchmark = parts[i++]
            val mode = parts[i++].lowercase(Locale.US)
            val cnt = if (parts.size >= 5) parts[i++].toIntOrNull() ?: 1 else 1
            val score = parts[i++].toBigDecimal()
            var error = if (parts.size >= 6) parts[i++].trimStart('±', '�').trimStart().toBigDecimalOrNull() else null
            var units = parts[i++]

            // Workaround for Win problems:
            // https://github.com/fluxo-kt/fluxo-mvi/actions/runs/3840763260#summary-10443174139
            if (error == null && splitRegex in units) {
                val u = units.split(splitRegex, 2)
                error = u[0].toBigDecimalOrNull()
                units = u[1]
            }

            val clazz: String
            val name: String
            val dotIdx = benchmark.indexOf('.')
            if (dotIdx != -1) {
                clazz = benchmark.substring(0, dotIdx)
                name = benchmark.substring(dotIdx + 1, benchmark.length)
            } else {
                clazz = ""
                name = benchmark
            }

            check(i == parts.size) { "i=$i, parts.size=${parts.size}: '$line'" }

            JmhResult(clazz, name, mode, cnt, score, error, units)
        }.groupBy { it.clazz }.mapValues { byClass ->
            byClass.value.groupBy { it.mode }.mapValues { byMode ->
                when (byMode.key) {
                    "thrpt" -> byMode.value.sortedByDescending { it.score }
                    else -> byMode.value.sortedBy { it.score }
                }
            }.values.flatten()
        }

        println("### JMH Benchmark results ($total total)")
        val isCI = System.getenv("CI")?.lowercase(Locale.US) in arrayOf("1", "true")
        if (!isCI) {
            println()
            println()
        }

        val errTitle = "Error"
        val bdTwo = BigDecimal.valueOf(2)
        val titles = arrayOf("Benchmark", "Mode", "Cnt", "Score", errTitle, "Units")
        val errIndex = titles.indexOf(errTitle)
        val mdTitles = titles.filter { it != errTitle }.toTypedArray()
        for ((clazz, results) in resultsByClass) {
            println("#### ${clazz.ifEmpty { "<not set>" }} (${results.size} total)")

            // Table header
            if (isCI) {
                println(mdTitles.joinToString(" | ", "| ", " |"))
                println(mdTitles.mapIndexed { i, s ->
                    val dashes = "-".repeat(s.length)
                    // GFM Markdown sort cols to the right for 2nd+ columns
                    if (i == 0) "-$dashes-" else "-$dashes:"
                }.joinToString("|", "|", "|"))
            } else {
                println()
            }

            val maxLengths = IntArray(titles.size) { titles[it].length }
            var prevMode = results[0].mode
            for (r in results) {
                if (isCI) {
                    val template = "| %s | %s | %s | %s | %s |"
                    if (r.mode != prevMode) {
                        prevMode = r.mode
                        println(template.format("", "", "", "", ""))
                    }

                    val score = r.score
                    val scoreWithError = r.error?.let {
                        // ±
                        val error = "<b>$score</b><sub><i> &#177; $it</i></sub>"
                        // ❌ Mark huge error
                        if (it >= score / bdTwo) "&#10060; $error" else error
                    } ?: "<b>$score</b>"

                    println(
                        template.format(
                            r.name,
                            "<sub>${r.mode}</sub>",
                            "<sub>${r.cnt}</sub>",
                            scoreWithError,
                            "<sub>${r.units}</sub>",
                        )
                    )
                }

                // max length calculation for each field
                r.asRawArray.forEachIndexed { i, v ->
                    val len = v.toString().length
                    if (maxLengths[i] < len) {
                        maxLengths[i] = len
                    }
                }
            }

            // Raw results
            if (isCI) {
                println("<details><summary><i>Raw results</i></summary><p><pre language=\"jmh\">")
            }
            fun Int.f(v: Any, entity: Boolean = false): String {
                val s = v.toString()
                val spaces = " ".repeat(maxLengths[this] - s.length)
                val fs = if (entity && this == errIndex && isCI) s.replace("±", "&#177;") else s
                return when (this) {
                    0 -> "$fs$spaces"
                    errIndex -> "$spaces$fs"
                    else -> " $spaces$fs"
                }
            }
            println(titles.mapIndexed { i, s -> i.f(s) }.joinToString(" "))
            prevMode = results[0].mode
            for (r in results) {
                if (r.mode != prevMode) {
                    prevMode = r.mode
                    println()
                }

                // ❌ Mark huge error
                val errorMark = r.error.let {
                    if (it == null || it < r.score / bdTwo) "" else when {
                        isCI -> " &#10060;"
                        else -> " ❌"
                    }
                }

                println(r.asRawArray.mapIndexed { i, v -> i.f(v, entity = true) }.joinToString(" ", postfix = errorMark))
            }
            if (isCI) {
                println("</pre></p></details>")
            } else {
                println()
                println()
            }
        }
    } else {
        System.err.println("JMH results NOT found: $koverFile")
    }
} catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
    System.err.println("JMH results error: $e")
    @Suppress("PrintStackTrace")
    e.printStackTrace(System.err)
}
