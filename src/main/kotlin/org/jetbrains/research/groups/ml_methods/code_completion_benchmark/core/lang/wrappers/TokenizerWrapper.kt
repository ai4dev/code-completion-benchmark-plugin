package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.wrappers

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.Tokenizer
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.Vocabulary

class TokenizerWrapper(
        val tokenizer: Tokenizer,
        val isPerLine: Boolean
) {

    var useSentenceMarkers = false

    fun willLexFile(file: PsiFile?): Boolean {
        file ?: return false
        return file.language == JavaLanguage.INSTANCE
    }

    fun lexDirectory(directory: PsiDirectory): Sequence<Pair<PsiFile, Sequence<Sequence<String>>>>? {
        val files = PsiTreeUtil
                .collectElements(directory) { it is PsiFile }
                .asSequence()
                .map { it as? PsiFile }
                .filter { willLexFile(it) }
                .filterNotNull()
        return files.map { fIn ->
            Pair(fIn, lexFile(fIn))
        }
    }

    fun lexFile(file: PsiFile): Sequence<Sequence<String>> {
        return if (!willLexFile(file)) sequenceOf() else lexTokens(tokenizer.tokenizeFile(file))
    }

    private fun lexTokens(tokens: Sequence<Sequence<String>>): Sequence<Sequence<String>> {
        return if (useSentenceMarkers)
            lexWithDelimiters(tokens)
        else
            tokens
    }

    private fun lexWithDelimiters(lexed: Sequence<Sequence<String>>): Sequence<Sequence<String>> {
        return if (isPerLine)
            lexed.map { sequenceOf(Vocabulary.BEGIN_STRING) + it + sequenceOf(Vocabulary.END_STRING) }
        else
            sequenceOf(sequenceOf(Vocabulary.BEGIN_STRING)) + lexed + sequenceOf(sequenceOf(Vocabulary.BEGIN_STRING))

    }
}