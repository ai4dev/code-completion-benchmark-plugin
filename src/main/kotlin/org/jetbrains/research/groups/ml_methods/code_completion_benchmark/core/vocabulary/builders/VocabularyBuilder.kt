package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.builders

import com.intellij.psi.PsiDirectory

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.io.Reader
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.wrappers.TokenizerWrapper
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.TokenVocabulary

import java.io.*
import java.nio.charset.StandardCharsets

object VocabularyBuilder {
    private var cutOff = 0
        set(value) {
            var cut = value
            if (value < 0) {
                cut = 0
            }
            field = cut
        }

    fun build(tokenizerWrapper: TokenizerWrapper, root: PsiDirectory): TokenVocabulary {
        val vocabulary = TokenVocabulary()

        val counts = tokenizerWrapper
                .lexDirectory(root)!!
                .flatMap { it.second }
                .flatten()
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
        vocabulary.store(TokenVocabulary.UNKNOWN_TOKEN,
                         vocabulary.getCount(TokenVocabulary.UNKNOWN_TOKEN)!! + unkCount)

        return vocabulary
    }

    fun read(file: File): TokenVocabulary {
        val vocabulary = TokenVocabulary()

        Reader.readLines(file)
                .map { it.split("\t".toRegex(), 3) }
                .filter { it[0].toInt() >= cutOff }
                .forEach { split ->
                    val count = split[0].toInt()
                    val token = split[2]
                    vocabulary.store(token, count)
                }

        return vocabulary
    }

    fun write(tokenVocabulary: TokenVocabulary, file: File) {
        try {
            BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { fw ->
                for (i in 0 until tokenVocabulary.size()) {
                    val count = tokenVocabulary.counts[i]
                    val word = tokenVocabulary.words[i]
                    fw.append(count.toString() + "\t" + i + "\t" + word + "\n")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}