package io.ksmt.solver.neurosmt.ksmtBinaryConverter

import io.ksmt.KContext
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.neurosmt.FormulaGraphExtractor
import io.ksmt.solver.neurosmt.deserialize
import io.ksmt.solver.neurosmt.getAnswerForTest
import me.tongfei.progressbar.ProgressBar
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.time.Duration.Companion.seconds

// tool to convert formulas from ksmt binary format to their structure graphs
fun main(args: Array<String>) {
    val inputRoot = args[0]
    val outputRoot = args[1]
    val timeout = args[2].toInt().seconds

    if (args[3] !in listOf("graph", "raw")) {
        throw IllegalArgumentException("specify output format (raw/graph)")
    }
    val graphOutput = args[3] == "graph"
    val simplify = args.getOrNull(4) == "simplify"

    val files = Files.walk(Path.of(inputRoot)).filter { it.isRegularFile() }

    File(outputRoot).mkdirs()

    var sat = 0; var unsat = 0; var skipped = 0

    val ctx = KContext(
        simplificationMode = if (simplify) {
            KContext.SimplificationMode.SIMPLIFY
        } else {
            KContext.SimplificationMode.NO_SIMPLIFY
        },
        astManagementMode = KContext.AstManagementMode.GC
    )

    var curIdx = 0
    ProgressBar.wrap(files.toList(), "converting ksmt binary files").forEach {
        val assertList = try {
            deserialize(ctx, FileInputStream(it.toFile()))
        } catch (e: Exception) {
            skipped++
            return@forEach
        }

        val answer = when {
            it.name.endsWith("-sat") -> KSolverStatus.SAT
            it.name.endsWith("-unsat") -> KSolverStatus.UNSAT
            else -> getAnswerForTest(ctx, assertList, timeout)
        }

        if (answer == KSolverStatus.UNKNOWN) {
            skipped++
            return@forEach
        }

        val relFile = it.toFile().relativeTo(File(inputRoot))
        val parentDirFile = if (relFile.parentFile == null) {
            "."
        } else {
            relFile.parentFile.path
        }
        val outputDir = File(outputRoot, parentDirFile)
        outputDir.mkdirs()

        with(ctx) {
            val formula = when (assertList.size) {
                0 -> {
                    skipped++
                    return@forEach
                }
                1 -> {
                    assertList[0]
                }
                else -> {
                    mkAnd(assertList)
                }
            }

            val outputFile = File("$outputDir/$curIdx-${answer.toString().lowercase()}")
            val outputStream = FileOutputStream(outputFile)

            if (graphOutput) {
                outputStream.write("; $it\n".encodeToByteArray())
                val extractor = FormulaGraphExtractor(ctx, formula, outputStream)
                extractor.extractGraph()
            } else {
                val formulaString = StringBuilder()
                formula.print(formulaString)
                outputStream.write(formulaString.toString().toByteArray())
            }
        }

        when (answer) {
            KSolverStatus.SAT -> sat++
            KSolverStatus.UNSAT -> unsat++
            else -> { /* can't happen */ }
        }

        curIdx++
    }

    println()
    println("sat: $sat; unsat: $unsat; skipped: $skipped")
}