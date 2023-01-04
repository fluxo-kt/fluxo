#!/usr/bin/env kotlin

import java.io.File
import java.math.BigDecimal
import java.util.Locale

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
                get() = arrayOf(
                    "${clazz}.$name", mode, cnt, score, error?.let { "± $it" } ?: "", units,
                )
        }

        val splitRegex = Regex("(?i)(?<![±�])\\s+")
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
            val error = if (parts.size >= 6) parts[i++].trimStart('±', '�').trimStart().toBigDecimalOrNull() else null
            val units = parts[i++]

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

        println("#### JMH Benchmark results ($total total)")

        val titles = arrayOf("Benchmark", "Mode", "Cnt", "Score", "Error", "Units")
        for ((clazz, results) in resultsByClass) {
            println("##### ${clazz.ifEmpty { "<not set>" }}")

            // Table header
            println(titles.joinToString(" | ", "| ", " |"))
            println(titles.joinToString(" | ", "| ", " |") { "-".repeat(it.length) })

            val maxLengths = IntArray(titles.size) { titles[it].length }
            val bdTwo = BigDecimal.valueOf(2)
            for (r in results) {
                val score = r.score
                // ±; ❌ Mark huge error
                val error = r.error?.let { if (it != null && it >= score / bdTwo) "&#10060; &#177; $it" else "&#177; $it" } ?: ""

                println(
                    "| %s | %s | %s | %s | %s | %s |"
                        .format(r.name, r.mode, r.cnt, score, error, r.units)
                )

                // max length calculation for each field
                r.asRawArray.forEachIndexed { i, v ->
                    val len = v.toString().length
                    if (maxLengths[i] < len) {
                        maxLengths[i] = len
                    }
                }
            }

            // Raw results
            println("<details><summary><i>Raw results</i></summary><p><pre language=\"txt\">")
            println(titles.mapIndexed { i, s ->
                val spaces = " ".repeat(maxLengths[i] - s.length)
                if (i == 0) "$s$spaces" else "$spaces$s"
            }.joinToString(" "))
            for (r in results) {
                println(r.asRawArray.mapIndexed { i, v ->
                    val s = v.toString()
                    val spaces = " ".repeat(maxLengths[i] - s.length)
                    val fs = if (i == 4) s.replace("±", "&#177;") else s
                    if (i == 0) "$fs$spaces" else "$spaces$fs"
                }.joinToString(" "))
            }
            println("</pre></p></details>")
        }
    } else {
        System.err.println("JMH results NOT found: $koverFile")
    }
} catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
    System.err.println("JMH results error: $e")
    @Suppress("PrintStackTrace")
    e.printStackTrace(System.err)
}
