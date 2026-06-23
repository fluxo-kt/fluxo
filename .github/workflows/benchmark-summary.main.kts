#!/usr/bin/env kotlin

import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
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

        val splitRegex =
            Pattern.compile("(?i)(?<![±�])\\s+", Pattern.UNICODE_CHARACTER_CLASS or Pattern.UNICODE_CASE).toRegex()
        val bd100 = BigDecimal(100)
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
        System.err.println("JMH results NOT found: $koverFile")
    }
} catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
    System.err.println("JMH results error: $e")
    @Suppress("PrintStackTrace")
    e.printStackTrace(System.err)
}


fun Int.enPlural(one: String, other: String): String {
    return (if (this == 1) one else other).format(this)
}


// JMH dual-gate baseline comparison (plan §11).
// Gated on JMH_BASELINE_CHECK=1 so non-gating runs (PR summary table, local
// devs) stay unaffected. Fires only when the baseline-comparison CI step
// opts in. Reads host-matched `benchmarks/jmh/baselines/main-${os}.json`,
// where `os` is `darwin` on macOS runners and `linux` on Linux runners.
// Per-benchmark dual gate: `|delta|/baselineStderr > 2.0 AND |delta|/baseline > 5%`.
// Both conditions necessary — effect must be both statistically significant
// AND non-trivial. Either alone is noise.
// Exits with code 1 if any matched benchmark fails the gate, blocking merge.
// Missing baseline file is treated as "no gate" (warn + exit 0), so first-run
// after fresh-baseline regeneration in CI doesn't self-block.
if (System.getenv("JMH_BASELINE_CHECK")?.lowercase(Locale.US) in arrayOf("1", "true")) run check@ {
    try {
        val resultsFile = File("benchmarks/jmh/build/results/jmh/results.txt")
        if (!resultsFile.exists()) {
            System.err.println("[baseline-check] no current results.txt at ${resultsFile.path}; skipping gate")
            return@check
        }
        val osName = System.getProperty("os.name").lowercase(Locale.US)
        val osKey = when {
            "mac" in osName || "darwin" in osName -> "darwin"
            "linux" in osName -> "linux"
            else -> {
                System.err.println("[baseline-check] unsupported os.name='$osName'; gate only runs on darwin|linux")
                return@check
            }
        }
        val baselineFile = File("benchmarks/jmh/baselines/main-$osKey.json")
        if (!baselineFile.exists()) {
            System.err.println("[baseline-check] no baseline at ${baselineFile.path}; gate skipped (regenerate baseline in CI to enable)")
            return@check
        }

            // Minimal JMH-JSON parser. JMH's `-rf json` emits a top-level array
            // of objects with shallow keys; we only need (benchmark, mode,
            // primaryMetric.score, primaryMetric.scoreError). Per-entry regex
            // tolerates whitespace, scientific notation, and field reordering.
            data class BaselineEntry(val benchmark: String, val mode: String, val score: BigDecimal, val scoreError: BigDecimal)
            val baselineText = baselineFile.readText()
            // Allow one level of nesting (primaryMetric / secondaryMetrics are nested objects).
            // Two levels would need a recursive regex (Kotlin/Java regex doesn't support); JMH's
            // schema is shallow enough that one level suffices for the (benchmark, mode, score,
            // scoreError) fields we read.
            val entryRegex = Regex("\\{(?:[^{}]|\\{[^{}]*\\})*?\"benchmark\"(?:[^{}]|\\{[^{}]*\\})*?\\}", RegexOption.DOT_MATCHES_ALL)
            val benchmarkRx = Regex("\"benchmark\"\\s*:\\s*\"([^\"]+)\"")
            val modeRx = Regex("\"mode\"\\s*:\\s*\"([^\"]+)\"")
            val scoreRx = Regex("\"score\"\\s*:\\s*([\\d.eE+\\-]+)")
            val errorRx = Regex("\"scoreError\"\\s*:\\s*([\\d.eE+\\-]+)")
            val baseline = entryRegex.findAll(baselineText).mapNotNull { m ->
                val raw = m.value
                val b = benchmarkRx.find(raw)?.groupValues?.get(1) ?: return@mapNotNull null
                val mo = modeRx.find(raw)?.groupValues?.get(1) ?: return@mapNotNull null
                val sc = scoreRx.find(raw)?.groupValues?.get(1)?.toBigDecimalOrNull() ?: return@mapNotNull null
                val er = errorRx.find(raw)?.groupValues?.get(1)?.toBigDecimalOrNull() ?: return@mapNotNull null
                BaselineEntry(b, mo, sc, er)
            }.associateBy {
                // results.txt emits "SimpleClass.method"; JMH JSON emits FQN
                // "pkg.SimpleClass.method". Normalise both sides to last two
                // dot-segments so the join works regardless of source format.
                val simpleFqn = it.benchmark.split('.').takeLast(2).joinToString(".")
                "$simpleFqn|${it.mode.lowercase(Locale.US)}"
            }

        if (baseline.isEmpty()) {
            System.err.println("[baseline-check] baseline ${baselineFile.path} parsed to 0 entries — refusing to gate (parser regression vs JMH schema change?)")
            return@check
        }

            // Re-parse current results.txt — duplicates the parser above
            // because that parser's state-by-class is bound to the summary
            // pipeline. Cheap to repeat (a few dozen lines).
            data class CurrentEntry(val fqn: String, val mode: String, val score: BigDecimal, val error: BigDecimal?)
            val splitRx = Pattern.compile("(?i)(?<![±�])\\s+", Pattern.UNICODE_CHARACTER_CLASS or Pattern.UNICODE_CASE).toRegex()
            val current = resultsFile.readText().lineSequence().drop(1).mapNotNull { line ->
                val l = line.trim()
                if (l.isEmpty()) return@mapNotNull null
                val parts = l.split(splitRx, 6)
                if (parts.size < 4) return@mapNotNull null
                var i = 0
                val fqn = parts[i++]
                val mode = parts[i++].lowercase(Locale.US)
                if (parts.size >= 5) i++ // skip cnt
                val sc = parts.getOrNull(i++)?.toBigDecimalOrNull() ?: return@mapNotNull null
                val er = parts.getOrNull(i)?.trimStart('±', '�')?.trimStart()?.toBigDecimalOrNull()
                CurrentEntry(fqn, mode, sc, er)
            }.toList()

            val bd100 = BigDecimal(100)
            val twoBd = BigDecimal(2)
            val fivePctBd = BigDecimal("0.05")
            data class Verdict(val fqn: String, val mode: String, val curr: BigDecimal, val base: BigDecimal, val baseErr: BigDecimal, val passes: Boolean, val note: String)
            val verdicts = current.mapNotNull { c ->
                val simpleFqn = c.fqn.split('.').takeLast(2).joinToString(".")
                val key = "$simpleFqn|${c.mode}"
                val b = baseline[key] ?: return@mapNotNull null
                val delta = (c.score - b.score).abs()
                val deltaPctOfBase = if (b.score.signum() != 0) delta.divide(b.score, 6, RoundingMode.HALF_UP) else BigDecimal.ZERO
                val significant = b.scoreError.signum() != 0 && delta > b.scoreError * twoBd
                val nonTrivial = deltaPctOfBase > fivePctBd
                val passes = !(significant && nonTrivial)
                val pctStr = deltaPctOfBase.multiply(bd100).setScale(1, RoundingMode.HALF_DOWN)
                val sigmaStr = if (b.scoreError.signum() == 0) "∞" else delta.divide(b.scoreError, 2, RoundingMode.HALF_UP).toPlainString()
                val note = "Δ=$pctStr%, ${sigmaStr}σ"
                Verdict(c.fqn, c.mode, c.score, b.score, b.scoreError, passes, note)
            }

            if (verdicts.isEmpty()) {
                System.err.println("[baseline-check] no current ↔ baseline matches found (current entries=${current.size}, baseline entries=${baseline.size}); refusing to gate silently")
                System.exit(1)
            }

            val failed = verdicts.filterNot { it.passes }
            val isCI = System.getenv("CI")?.lowercase(Locale.US) in arrayOf("1", "true")
            println()
            println("### JMH baseline gate — host `$osKey` (${verdicts.size} compared, ${failed.size} failed)")
            if (isCI) {
                println()
                println("| Benchmark | Mode | Current | Baseline | Δ | Verdict |")
                println("|-----------|:----:|--------:|---------:|---|:-------:|")
                for (v in verdicts) {
                    val mark = if (v.passes) "✅" else "❌"
                    println("| `${v.fqn}` | ${v.mode} | ${v.curr} | ${v.base} ±${v.baseErr} | ${v.note} | $mark |")
                }
            }
            for (v in failed) {
                System.err.println("[baseline-check] FAIL ${v.fqn} (${v.mode}): current=${v.curr}, baseline=${v.base}±${v.baseErr}, ${v.note}")
            }
            if (failed.isNotEmpty()) {
                System.err.println("[baseline-check] ${failed.size} benchmark(s) regressed past dual gate (|Δ|/σ>2 AND |Δ|/base>5%) — blocking merge")
                System.exit(1)
            }
            System.err.println("[baseline-check] all ${verdicts.size} matched benchmarks within dual gate")
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        System.err.println("[baseline-check] unexpected error: $e")
        @Suppress("PrintStackTrace")
        e.printStackTrace(System.err)
        System.exit(1)
    }
}
