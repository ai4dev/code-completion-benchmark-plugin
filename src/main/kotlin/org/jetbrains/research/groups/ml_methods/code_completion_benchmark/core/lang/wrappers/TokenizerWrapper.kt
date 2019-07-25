package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.wrappers

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.Tokenizer
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.Vocabulary

class TokenizerWrapper(val tokenizer: Tokenizer, val isPerLine: Boolean) {

    var sentenceMarkers = false

    fun willLexFile(file: PsiFile): Boolean {
        return file.language == JavaLanguage.INSTANCE
    }

    fun lexDirectory(directory: PsiDirectory): List<Pair<PsiFile, Sequence<Sequence<String>>>>? {
        val files = PsiTreeUtil
                .collectElements(directory) { element -> element is PsiFile }
                .map { it as PsiFile }
                .filter { willLexFile(it) }

        return files
                .map { fIn ->
                    Pair(fIn, lexFile(fIn))
                }
    }

    fun lexFile(file: PsiFile): Sequence<Sequence<String>> {
        return when (!willLexFile(file)) {
            true  -> emptySequence()
            false -> {
                lexTokens(tokenizer.tokenizeFile(file))
            }
        }
    }

    private fun lexTokens(tokens: Sequence<Sequence<String>>): Sequence<Sequence<String>> {
        return if (sentenceMarkers) lexWithDelimiters(tokens) else tokens
    }

    private fun lexWithDelimiters(lexed: Sequence<Sequence<String>>): Sequence<Sequence<String>> {
        return when (isPerLine) {
            true  -> lexed.map { sequenceOf(Vocabulary.BEGIN_STRING) + it + sequenceOf(Vocabulary.END_STRING) }
            false -> sequenceOf(sequenceOf(Vocabulary.BEGIN_STRING)) + lexed + sequenceOf(sequenceOf(Vocabulary.END_STRING))
        }

    }
}