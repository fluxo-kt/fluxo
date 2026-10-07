#!/usr/bin/env kotlin

import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.regex.Pattern

// JMH `results.txt` column splitter: any whitespace except after a σ-marker.
// The replacement char `�` covers Windows-host encoding fallouts where '±' lost
// its glyph (see actions run 3840763260). Hoisted so the summary + baseline-gate
// paths can't diverge on this pattern.
val splitRegex = Pattern.compile("(?i)(?<![±�])\\s+", Pattern.UNICODE_CHARACTER_CLASS or Pattern.UNICODE_CASE).toRegex()

// JMH results summary
try {
    val jmhResultsFile = File("benchmarks/jmh/build/results/jmh/results.txt")
    if (jmhResultsFile.exists()) {
        System.err.println("JMH results FOUND: $jmhResultsFile")

        val text = jmhResultsFile.readText()

        data class JmhResult(
            val clazz: String,
            val name: String,
            val mode: String,
            val cnt: Int,
            val score: BigDecimal,
            val error: BigDecimal?,
            val units: String,
        ) {
            var percent: BigDecimal = BigDecimal.ZERO
                get() = field.setScale(1, RoundingMode.HALF_DOWN)

            val asRawArray: Array<Any?>
                get() = arrayOf(
                    name,
                    mode,
                    cnt.let { if (it == 1) "" else "$it" },
                    score,
                    error ?: "",
                    units,
                    "$percent%",
                )
        }

        val bd100 = BigDecimal(100)
        var total = 0
        val resultsByClass = text.lineSequence().drop(1).mapNotNull l@{ line ->
            // JMH formats numbers in the host locale (`0,105` on a comma-decimal machine) and never groups thousands.
            val l = line.trim().replace(',', '.')
            // Profiler rows (`Benchmark:gc.alloc.rate.norm`) are not benchmarks: the regression gate below reads them.
            if (l.isEmpty() || ':' in l.substringBefore(' ')) {
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
            // https://github.com/fluxo-kt/fluxo/actions/runs/3840763260#summary-10443174139
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
                val isReverse = byMode.key == "thrpt"
                val results = when {
                    isReverse -> byMode.value.sortedByDescending { it.score }
                    else -> byMode.value.sortedBy { it.score }
                }
                val best = (results.firstOrNull { !it.name.contains("naive", ignoreCase = true) }
                    ?: results.first()).score
                for (r in results) {
                    r.percent = r.score / best * bd100 - bd100
                }
                results
            }.values.flatten()
        }

        println("### JMH Benchmark results ($total ${total.enPlural("test", "tests")} total)")
        val isCI = System.getenv("CI")?.lowercase(Locale.US) in arrayOf("1", "true")
        if (!isCI) {
            println()
            println()
        }

        val bnchmrkTitle = "Benchmark"
        val modeTitle = "Mode"
        val cntTitle = "Cnt"
        val errTitle = "Error"
        val unitTitle = "Units"
        val bdTwo = BigDecimal.valueOf(2)
        val titles = arrayOf(bnchmrkTitle, modeTitle, cntTitle, "Score", errTitle, unitTitle, "Percent")
        val leftAlign = hashSetOf(bnchmrkTitle, unitTitle)
        val centerAlign = hashSetOf(modeTitle)
        val errIndex = titles.indexOf(errTitle)
        val mdTitles = titles.filter { it != errTitle }.toTypedArray()
        for ((clazz, results) in resultsByClass) {
            val modes = results.distinctBy { it.mode }.size
            val modeInfo = " in " + modes.enPlural(
                if (isCI) "<u>one</u> mode" else "one mode",
                if (isCI) "<u>%d</u> modes" else "%d modes",
            )

            val iterInfo = results.distinctBy { it.cnt }.let {
                when (it.size) {
                    1 -> " with " + it[0].cnt.enPlural(
                        if (isCI) "<u>one</u> iteration" else "one iteration",
                        if (isCI) "<u>%d</u> iterations" else "%d iterations",
                    )

                    else -> ""
                }
            }
            val skipCnt = iterInfo.isNotEmpty()

            val testsInfo = (results.size / modes).enPlural(
                if (isCI) "<u>%d</u> test" else "%d test",
                if (isCI) "<u>%d</u> tests" else "%d tests",
            )
            println("#### ${clazz.ifEmpty { "<not set>" }} ($testsInfo$modeInfo$iterInfo)")

            if (clazz.equals("IncrementIntentBenchmark", ignoreCase = true)) {
                println("\n> _Each **operation** creates a state store, sends 5000 intents with reduction, and checks state updates!_\n")
            }

            // Table header
            val mdTitles0 = if (skipCnt) mdTitles.filter { it != cntTitle }.toTypedArray() else mdTitles

            if (isCI) {
                println(mdTitles0.joinToString(" | ", "| ", " |"))
                println(mdTitles0.joinToString("|", "|", "|") { title ->
                    val dashes = "-".repeat(title.length)
                    // GFM cols sort
                    when (title) {
                        in leftAlign -> "-$dashes-"
                        in centerAlign -> ":$dashes:"
                        else -> "-$dashes:" // right align
                    }
                })
            } else {
                println()
            }

            val maxLengths = IntArray(titles.size) { titles[it].length }
            var prevMode = results[0].mode
            for (r in results) {
                if (isCI) {
                    val template = "| %s ".repeat(mdTitles0.size) + '|'
                    if (r.mode != prevMode) {
                        prevMode = r.mode
                        @Suppress("SpreadOperator")
                        println(template.format(*Array(mdTitles0.size) { "" }))
                    }

                    val score = r.score
                    val scoreWithError = r.error?.let {
                        // ±
                        val error = "<b>$score</b><sub><i> &#177; $it</i></sub>"
                        // ❌ Mark huge error
                        if (it >= score / bdTwo) "&#10060; $error" else error
                    } ?: "<b>$score</b>"

                    val values = listOfNotNull(
                        r.name,
                        "<sub>${r.mode}</sub>",
                        if (!skipCnt) "<sub>${r.cnt}</sub>" else null,
                        scoreWithError,
                        "<sub>${r.units}</sub>",
                        "<sub><i>${r.percent}%</i></sub>",
                    ).toTypedArray()

                    @Suppress("SpreadOperator")
                    println(template.format(*values))
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
                print("<details><summary><i>Raw results</i></summary><p><pre language=\"jmh\">\n")
            }
            fun Int.f(v: Any?, entity: Boolean = false): String {
                val s = v.toString()
                val spaces = " ".repeat(maxLengths[this] - s.length)
                val e = when {
                    this != errIndex -> ""
                    !entity || s.isEmpty() -> "  "
                    isCI -> "&#177; "
                    else -> "± "
                }
                return when (this) {
                    0 -> "$s$spaces"
                    errIndex -> "$e$spaces$s"
                    else -> " $spaces$s"
                }
            }
            print(titles.mapIndexed { i, s -> i.f(s) }.joinToString(" ") + '\n')
            prevMode = results[0].mode
            for (r in results) {
                if (!isCI && r.mode != prevMode) {
                    prevMode = r.mode
                    print("\n")
                }

                // ❌ Mark huge error
                val errorMark = r.error.let {
                    when {
                        it == null || it < r.score / bdTwo -> ""
                        isCI -> " &#10060;"
                        else -> " ❌"
                    }
                }

                val resultText = r.asRawArray
                    .mapIndexed { i, v -> i.f(v, entity = true) }
                    .joinToString(" ", postfix = errorMark + '\n')
                print(resultText)
            }
            if (isCI) {
                print("</pre></p></details>\n")
            } else {
                print("\n\n")
            }
        }
    } else {
        System.err.println("JMH results NOT found: $jmhResultsFile")
    }
} catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
    System.err.println("JMH results error: $e")
    @Suppress("PrintStackTrace")
    e.printStackTrace(System.err)
}


fun Int.enPlural(one: String, other: String): String {
    return (if (this == 1) one else other).format(this)
}


// Fluxo regression gate: bytes allocated per operation (`:gc.alloc.rate.norm`) by this run's Fluxo rows, against the
// same rows built from benchmark.yml's JMH_REFERENCE commit in the same job (`results-reference.txt`).
// Why allocation, not time: time on a shared CI runner is noise. Identical code measured twice in one job differed by
// 22–81 % on macOS (run 37687829186) and by 3× on a loaded workstation, so any time threshold either fails at random or
// misses real slowdowns. Bytes per op repeated to 0.0005 % in the same experiment, and extra allocation is how
// coroutine state code usually gets slower. Cost: a slowdown that allocates nothing passes; the time table above shows it.
// Why a reference build in the same job, not stored numbers: allocation shifts with the JDK and the coroutines version,
// so a stored number goes stale on every toolchain bump, while both sides of a same-job comparison move together.
// Enabled by JMH_REFERENCE_CHECK=1 (benchmark.yml), so local runs only print the table above.
if (System.getenv("JMH_REFERENCE_CHECK")?.lowercase(Locale.US) in arrayOf("1", "true")) run check@ {
    try {
        data class Row(val benchmark: String, val mode: String, val score: BigDecimal, val error: BigDecimal)

        // Columns: Benchmark Mode [Cnt] Score [± Error] Units. Cnt and Error are absent for a single measurement.
        fun File.rows(): List<Row> = readText().lineSequence().drop(1).mapNotNull { line ->
            val parts = line.trim().replace(',', '.').split(splitRegex) // Comma decimals: see the table parser above.
            if (parts.size < 4) return@mapNotNull null
            val errorAt = parts.indexOfFirst { it.startsWith('±') || it.startsWith('�') }
            val scoreAt = if (errorAt > 0) errorAt - 1 else parts.size - 2
            val score = parts[scoreAt].toBigDecimalOrNull() ?: return@mapNotNull null
            // JMH right-aligns the error, so after a padded '±' it arrives as the next token.
            val errorText = if (errorAt > 0) parts[errorAt].drop(1).trim().ifEmpty { parts.getOrElse(errorAt + 1) { "" } } else ""
            val error = errorText.toBigDecimalOrNull() ?: BigDecimal.ZERO
            Row(parts[0], parts[1].lowercase(Locale.US), score, error)
        }.toList()

        val dir = "benchmarks/jmh/build/results/jmh"
        val referenceFile = File("$dir/results-reference.txt")
        if (!referenceFile.exists()) {
            System.err.println("[reference-check] no ${referenceFile.path}: the reference run did not complete (see its step log)")
            System.exit(1)
        }
        val alloc = ":gc.alloc.rate.norm"
        val reference = referenceFile.rows().filter { it.benchmark.endsWith(alloc) }.associateBy { it.benchmark to it.mode }
        val current = File("$dir/results.txt").rows().filter { it.benchmark.endsWith(alloc) }

        val bd100 = BigDecimal(100)
        // Compares means and ignores the error column: some rows alternate between two allocation paths per iteration
        // (mvi_handler: ±15 % error), yet their mean repeated to 0.00001 %. 1 % is far above that and far below an
        // added object per intent.
        val tolerance = BigDecimal("0.01")
        data class Verdict(val row: Row, val ref: Row, val passes: Boolean, val note: String)
        val verdicts = current.mapNotNull { c ->
            val r = reference[c.benchmark to c.mode] ?: return@mapNotNull null
            val delta = c.score - r.score
            val deltaPct = if (r.score.signum() != 0) delta.divide(r.score, 6, RoundingMode.HALF_UP) else BigDecimal.ZERO
            // One-sided: only more allocation fails; less is reported.
            val passes = deltaPct <= tolerance
            val pctStr = deltaPct.multiply(bd100).setScale(2, RoundingMode.HALF_UP)
            Verdict(c, r, passes, "${if (delta.signum() > 0) "+" else ""}$pctStr%")
        }
        if (verdicts.isEmpty()) {
            System.err.println("[reference-check] no rows match between results.txt (${current.size}) and ${referenceFile.name} (${reference.size}); refusing to pass silently")
            System.exit(1)
        }

        val failed = verdicts.filterNot { it.passes }
        println()
        println("### Fluxo allocation vs reference ${System.getenv("JMH_REFERENCE")?.take(10)} (${verdicts.size} compared, ${failed.size} failed)")
        println()
        println("| Benchmark | Mode | Current B/op | Reference B/op | Δ | Verdict |")
        println("|-----------|:----:|-------------:|---------------:|--:|:-------:|")
        for (v in verdicts) {
            val name = v.row.benchmark.removeSuffix(alloc)
            println("| `$name` | ${v.row.mode} | ${v.row.score} | ${v.ref.score} | ${v.note} | ${if (v.passes) "✅" else "❌"} |")
        }
        if (failed.isNotEmpty()) {
            for (v in failed) System.err.println("[reference-check] FAIL ${v.row.benchmark} (${v.row.mode}): ${v.row.score} B/op vs reference ${v.ref.score}, ${v.note}")
            System.err.println("[reference-check] allocates more than 1% above the reference commit. Find the new allocation with `-prof gc` locally; if it is intended, move JMH_REFERENCE in benchmark.yml to this commit.")
            System.exit(1)
        }
        System.err.println("[reference-check] all ${verdicts.size} rows within the gate")
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        System.err.println("[reference-check] unexpected error: $e")
        @Suppress("PrintStackTrace")
        e.printStackTrace(System.err)
        System.exit(1)
    }
}
