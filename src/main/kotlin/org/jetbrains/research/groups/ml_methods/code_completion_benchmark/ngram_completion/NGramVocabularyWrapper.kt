package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram_completion

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.TokenVocabulary
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.toolkit.vocabulary.Vocabulary

class NGramVocabularyWrapper : Vocabulary<Int> {
    private var tokenVocabulary: TokenVocabulary = TokenVocabulary()

    override fun translateCodePiece(codePiece: Any): Iterable<Int> {
        val tokens = (codePiece as? List<*>)?.filterIsInstance<String>() ?: return emptyList()
        return tokenVocabulary.toIndices(tokens)
    }

    override fun translateToken(name: String): Int? {
        return tokenVocabulary.toIndex(name)
    }

    override fun translateTokenBack(token: Int): String {
        return tokenVocabulary.toWord(token)
    }

    override fun getVocabularySize(): Int {
        return tokenVocabulary.size()
    }

    fun setCheckpoint() {
        tokenVocabulary.checkPoint = tokenVocabulary.words.size
    }

    fun restoreCheckpoint() {
        for (i in tokenVocabulary.words.size downTo tokenVocabulary.checkPoint + 1) {
            tokenVocabulary.counts.removeAt(tokenVocabulary.counts.size - 1)
            val word = tokenVocabulary.words.removeAt(tokenVocabulary.words.size - 1)
            tokenVocabulary.wordIndices.remove(word)
        }
    }
}