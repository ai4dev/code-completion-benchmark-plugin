package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.builders

import com.intellij.psi.PsiDirectory

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.io.Reader
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.wrappers.TokenizerWrapper
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.Vocabulary

import java.io.*
import java.nio.charset.StandardCharsets

object VocabularyBuilder {

    private val PRINT_FREQ = 1000000
    private var cutOff = 0
        set(value) {
            var cut = value
            if (value < 0) {
                cut = 0
            }
            field = cut
        }

    fun build(tokenizerWrapper: TokenizerWrapper, root: PsiDirectory): Vocabulary {
        val vocabulary = Vocabulary()
        val iterationCount = intArrayOf(0)

        val counts = tokenizerWrapper
                .lexDirectory(root)!!
                .flatMap { it.second }
                .flatten()
                .onEach {
                    if (++iterationCount[0] % PRINT_FREQ == 0)
                        System.out.printf(
                            "Building vocabulary, %dM tokens processed\n",
                            (iterationCount[0] / PRINT_FREQ).toFloat()
                        )
                }
                .groupingBy { it }
                .eachCount()

        val ordered = counts.entries.sortedByDescending { it.value }

        var unkCount = 0
        for ((token, count) in ordered) {
            if (count < cutOff) {
                unkCount += count
            } else {
                vocabulary.store(token, count)
            }
        }
        vocabulary.store(Vocabulary.UNKNOWN_TOKEN, vocabulary.getCount(Vocabulary.UNKNOWN_TOKEN)!! + unkCount)

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