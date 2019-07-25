package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.builders

import com.intellij.psi.PsiDirectory
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.io.Reader
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.wrappers.TokenizerWrapper
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.Vocabulary
import java.io.*
import java.nio.charset.StandardCharsets

object VocabularyBuilder {

    var cutOff = 0
        set(value) {
            field = when (value < 0) {
                true  -> 0
                false -> value
            }
        }

    fun build(tokenizerWrapper: TokenizerWrapper, root: PsiDirectory): Vocabulary {
        val vocabulary = Vocabulary()

        val counts = tokenizerWrapper
                .lexDirectory(root)!!
                .flatMap { it.second.asIterable() }
                .flatMap { it.asIterable() }
                .groupingBy { it }
                .eachCount()
                .mapKeys { it.key }

        val ordered = counts.entries.sortedByDescending { it.value }

        var unkCount = 0
        ordered.forEach { (token, count) ->
            when (count < cutOff) {
                true  -> unkCount += count
                false -> vocabulary.store(token, count)
            }
        }

        vocabulary.store(Vocabulary.UNKNOWN_TOKEN, vocabulary.getCount(Vocabulary.UNKNOWN_TOKEN)!! + unkCount)

        return vocabulary
    }

    fun read(file: File): Vocabulary {
        val vocabulary = Vocabulary()

        Reader.readLines(file)
                .map { it.split("\t".toRegex(), 3) }
                .filter { it[0].toInt() >= cutOff }
                .forEach { split ->
                    val count = split[0].toInt()
                    //val index = split[1].toInt()
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