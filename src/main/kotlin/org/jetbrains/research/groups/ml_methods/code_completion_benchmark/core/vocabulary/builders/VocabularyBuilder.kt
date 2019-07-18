package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.builders

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.io.Reader
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.tokenization.builders.TokenizerBuilder
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.Vocabulary
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object VocabularyBuilder {

    private var cutOff = 0
        set(value) {
            var cut = value
            if (cut < 0) cut = 0
            field = cut
        }

    fun build(tokenizerRunner: TokenizerBuilder, root: File): Vocabulary {
        val vocabulary = Vocabulary()

        val counts = tokenizerRunner
                .lexDirectory(root)!!
                .flatMap { it.second }
                .groupingBy { it }
                .eachCount()
                .mapKeys { it.toString() }

        val ordered = counts.entries.sortedByDescending { it.value }

        var unkCount = 0
        for ((token, count) in ordered) {
            if (count < cutOff) {
                unkCount += count
            } else {
                vocabulary.store(token, count)
            }
        }
        vocabulary.store(Vocabulary.UNK, vocabulary.getCount(Vocabulary.UNK)!! + unkCount)

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
                        println("VocabularyBuilder.read(): non-consecutive indices while reading vocabulary!")
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