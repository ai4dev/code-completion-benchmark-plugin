package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.tokenization.builders

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.tokenization.Tokenizer
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.Vocabulary
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.streams.asSequence

class TokenizerBuilder (val tokenizer: Tokenizer, val isPerLine: Boolean) {

    var sentenceMarkers = false
    var regex = ".*"
        set(value) {
            field = ".*\\.$value"
        }

    fun willLexFile(file: File): Boolean {
        return file.name.matches(regex.toRegex())
    }

    fun lexDirectory(directory: File): Sequence<Pair<File, Sequence<Sequence<String>>>>? {
        return try {
            Files.walk(directory.toPath())
                    .asSequence()
                    .map { it.toFile() }
                    .filter { it.isFile }
                    .filter { willLexFile(it) }
                    .map { fIn -> Pair(fIn, lexFile(fIn)) }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

    }

    fun lexFile(file: File): Sequence<Sequence<String>> {
        return if (!willLexFile(file))
            emptySequence()
        else
            lexTokens(tokenizer.tokenizeFile(file))
    }

    fun lexText(content: String): Sequence<Sequence<String>> {
        return lexTokens(tokenizer.tokenizeText(content))
    }

    fun lexLine(line: String): Sequence<String> {
        val lexed = tokenizer.tokenizeLine(line)

        return if (sentenceMarkers)
            sequenceOf(Vocabulary.BOS) + (lexed + sequenceOf(Vocabulary.EOS))
        else
            lexed

    }

    fun lexDirectory(from: File, to: File) {
        lexDirectoryToIndices(from, to, null)
    }

    fun lexDirectoryToIndices(from: File, to: File, vocabulary: Vocabulary?) {
        val count = intArrayOf(0)
        try {
            Files.walk(from.toPath())
                    .map{ it.toFile() }
                    .filter { it.isFile }
                    .forEach { fIn ->
                        if (++count[0] % 1000 == 0) {
                            println("Lexing at file " + count[0])
                        }
                        val path = to.absolutePath + fIn.absolutePath.substring(from.absolutePath.length)
                        val fOut = File(path)
                        val outDir = fOut.parentFile
                        outDir.mkdirs()

                        try {
                            val lexed = lexFile(fIn)
                            lexed.map { l -> l.map { w -> vocabulary?.store(w)?.toString() ?: w } }
                            //Writer.writeTokenized(fOut, lexed)
                        } catch (e: IOException) {
                            println("Exception in LexerBuilder.tokenize(), from $fIn to $fOut")
                            e.printStackTrace()
                        }
                    }
        } catch (e1: IOException) {
            e1.printStackTrace()
        }

    }


    private fun lexTokens(tokens: Sequence<Sequence<String>>): Sequence<Sequence<String>> {
        return if (sentenceMarkers)
            lexWithDelimiters(tokens)
        else
            tokens
    }

    private fun lexWithDelimiters(lexed: Sequence<Sequence<String>>): Sequence<Sequence<String>> {
        return if (isPerLine)
            lexed.map { sequenceOf(Vocabulary.BOS) + it + sequenceOf(Vocabulary.EOS) }
        else
            sequenceOf(sequenceOf(Vocabulary.BOS)) + lexed + sequenceOf(sequenceOf(Vocabulary.EOS))

    }
}