package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram_completion

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.TokenVocabulary
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.builders.VocabularyBuilder
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.toolkit.vocabulary.AbstractVocabularyWrapper
import java.io.File

class NGramVocabularyWrapper : AbstractVocabularyWrapper<Int, Iterable<Int>, TokenVocabulary>() {
    override var vocabulary: TokenVocabulary = TokenVocabulary()

    override fun translateCodePiece(codePiece: Any): Iterable<Int> {
        val tokens = (codePiece as? List<*>)?.filterIsInstance<String>() ?: return emptyList()
        return tokensListToRepresentations(tokens).asIterable().filterNotNull()
    }

    override fun getVocabularySize(): Int {
        return vocabulary.size()
    }

    fun setCheckpoint() {
        vocabulary.checkPoint = vocabulary.words.size
    }

    fun restoreCheckpoint() {
        for (i in vocabulary.words.size downTo vocabulary.checkPoint + 1) {
            vocabulary.counts.removeAt(vocabulary.counts.size - 1)
            val word = vocabulary.words.removeAt(vocabulary.words.size - 1)
            vocabulary.wordIndices.remove(word)
        }
    }

    override fun loadVocabulary(source: File): TokenVocabulary {
        return VocabularyBuilder.read(source)
    }

    override fun saveVocabulary(target: File) {
        VocabularyBuilder.write(vocabulary, target)
    }
}