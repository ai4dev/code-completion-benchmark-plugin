package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.builders

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.io.Reader
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.tokenization.wrappers.TokenizerWrapper
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.Vocabulary
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt

object VocabularyBuilder {

    private val PRINT_FREQ = 1000000

    var cutOff = 0
        set(value) {
            field = when(value < 0) {
                true -> 0
                false -> value
            }
        }

    fun build(tokenizerWrapper: TokenizerWrapper, root: File): Vocabulary {
        val vocabulary = Vocabulary()
        val iterationCount = intArrayOf(0)

        val counts = tokenizerWrapper
                .lexDirectory(root)!!
                .flatMap { it.second }
                .onEach {
                    if (++iterationCount[0] % PRINT_FREQ == 0)
                        System.out.printf(
                            "Building vocabulary, %dM tokens processed\n",
                            (iterationCount[0] / PRINT_FREQ).toFloat().roundToInt()
                        )
                }.asIterable()
                .groupingBy { it }
                .eachCount()
                .mapKeys { it.key.toString() }

        val ordered = counts.entries.sortedByDescending { it.value }

        var unkCount = 0
        ordered.forEach { (token, count) ->
            when (count < cutOff) {
                true -> unkCount += count
                false -> vocabulary.store(token, count)
            }
        }

        vocabulary.store(Vocabulary.UNK, vocabulary.getCount(Vocabulary.UNK)!! + unkCount)

        if (iterationCount[0] > PRINT_FREQ)
            println("Vocabulary constructed on ${iterationCount[0]} tokens, size: ${vocabulary.size()}")

        return vocabulary
    }

    fun read(file: File): Vocabulary {
        val vocabulary = Vocabulary()

        Reader.readLines(file)
                .map { it.split("\t".toRegex(), 3) }
                .filter { it[0].toInt() >= cutOff }
                .forEach { split ->
                    val count = split[0].toInt()
                    val index = split[1].toInt()
                    if (index > 0 && index != vocabulary.size()) {
                        println("VocabularyRunner.read(): non-consecutive indices while reading vocabulary!")
                    }
                    val token = split[2]
                    vocabulary.store(token, count)
                }

        return vocabulary
    }

    fun write(vocabulary: Vocabulary, file: File) {
        try {
            BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { fw ->
                for (i in 0 until vocabulary.size()) {
                    val count = vocabulary.counts[i]
                    val word = vocabulary.words[i]
                    fw.append(count.toString() + "\t" + i + "\t" + word + "\n")
                }
            }
        } catch (e: IOException) {
            println("Error writing vocabulary in Vocabulary.toFile()")
            e.printStackTrace()
        }

    }
}