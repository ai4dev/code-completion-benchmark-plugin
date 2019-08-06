package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram_completion

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.tokenizers.JavaTokenizer
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.wrappers.TokenizerWrapper
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.Completion
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.PredictionWithConf
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.toolkit.beans.Prediction
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.toolkit.model.AbstractModelRunner

import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.ln
import kotlin.streams.asStream

class NGramModelRunner : AbstractModelRunner() {
    override val id: String = NGramModelRunner::class.java.name

    override val modelWrapper = NGramModelWrapper()
    override val vocabularyWrapper = NGramVocabularyWrapper()

    private var tokenizerWrapper = TokenizerWrapper(JavaTokenizer(), true)
    private var selfTesting = false

    override fun getTopCodeSuggestion(codePiece: Any): Prediction? {
        val queryIndices = vocabularyWrapper.translateCodePiece(codePiece)
        val prediction = modelWrapper.predictToken(queryIndices.toList(), queryIndices.count() - 1)
                .toList()
                .maxBy { (_, value) -> value.probability }!!

        return Prediction(vocabularyWrapper.representationToTokenText(prediction.first), prediction.second.probability)
    }

    override fun getTopNCodeSuggestions(codePiece: Any, maxNumberOfSuggestions: Int): List<Prediction> {
        val queryIndices = vocabularyWrapper.translateCodePiece(codePiece)

        return modelWrapper.predictToken(queryIndices.toList(), queryIndices.count() - 1)
                .toList()
                .map { (key, value) -> Prediction(vocabularyWrapper.representationToTokenText(key), value.probability) }
                .sortedByDescending { (_, value) -> value }
                .take(maxNumberOfSuggestions)
    }

    fun learnDirectory(file: PsiDirectory) {
        tokenizerWrapper.lexDirectory(file)!!
                .forEach { p ->
                    modelWrapper.notify(p.first)
                    learnTokenSequence(p.second)
                }
    }

    fun learnFile(f: PsiFile) {
        if (!tokenizerWrapper.willLexFile(f))
            return

        modelWrapper.notify(f)
        learnTokenSequence(tokenizerWrapper.lexFile(f))
    }

    private fun learnTokenSequence(lexed: Sequence<Sequence<String>>) {
        if (tokenizerWrapper.isPerLine) {
            lexed
                    .map { vocabularyWrapper.translateCodePiece(it) }
                    .map { it.toList() }
                    .forEach { modelWrapper.learn(it) }
        } else {
            modelWrapper.learn(lexed
                    .flatMap { vocabularyWrapper.translateCodePiece(it).asSequence() }
                    .toList()
            )
        }
    }

    fun forgetDirectory(file: PsiDirectory) {
        file.files
                .filter { it.language == JavaLanguage.INSTANCE }
                .forEach { forgetFile(it) }
    }

    fun forgetFile(f: PsiFile) {
        if (!tokenizerWrapper.willLexFile(f))
            return

        modelWrapper.notify(f)
        forgetTokenSequence(tokenizerWrapper.lexFile(f))
    }

    private fun forgetTokenSequence(lexed: Sequence<Sequence<String>>) {
        if (tokenizerWrapper.isPerLine) {
            lexed.map { vocabularyWrapper.translateCodePiece(it) }
                    .map { it.toList() }
                    .forEach { modelWrapper.forget(it) }
        } else {
            modelWrapper.forget(
                lexed
                        .flatMap { vocabularyWrapper.translateCodePiece(it).asSequence() }
                        .toList()
            )
        }
    }

    fun modelDirectory(file: PsiDirectory): Sequence<Pair<PsiFile, List<List<Double>>>> {
        return tokenizerWrapper.lexDirectory(file)!!
                .map { p ->
                    modelWrapper.notify(p.first)
                    Pair(p.first, modelTokenSequence(p.second))
                }.asSequence()
    }

    fun modelFile(f: PsiFile): List<List<Double>>? {
        if (!tokenizerWrapper.willLexFile(f))
            return null

        modelWrapper.notify(f)
        return modelTokenSequence(tokenizerWrapper.lexFile(f))
    }

    private fun modelTokenSequence(lexed: Sequence<Sequence<String>>): List<List<Double>> {
        vocabularyWrapper
        val lineProbs: List<List<Double>>

        if (tokenizerWrapper.isPerLine) {
            lineProbs = lexed
                    .map { vocabularyWrapper.translateCodePiece(it) }
                    .map { it.toList() }
                    .map { modelTokens(it) }
                    .toList()
        } else {
            val lineLengths = ArrayList<Int>()

            val modeled = modelTokens(
                lexed
                        .map { vocabularyWrapper.translateCodePiece(it) }
                        .map { it.toList() }
                        .onEach { lineLengths.add(it.size) }
                        .flatMap { it.asSequence() }
                        .toList()
            )
            lineProbs = toLines(modeled, lineLengths)
        }

        vocabularyWrapper.restoreCheckpoint()
        return lineProbs
    }

    private fun modelTokens(tokens: List<Int>): List<Double> {
        if (selfTesting) modelWrapper.forget(tokens)

        val entropies = modelWrapper.model(tokens).stream()
                .map { PredictionWithConf.toProb(it, vocabularyWrapper.getVocabularySize()) }
                .map { toEntropy(it) }
                .collect(Collectors.toList())

        if (selfTesting)
            modelWrapper.learn(tokens)

        return entropies
    }

    fun predict(file: PsiDirectory): Sequence<Pair<PsiFile, List<List<Double>>>> {
        return tokenizerWrapper.lexDirectory(file)!!
                .map { p ->
                    modelWrapper.notify(p.first)
                    Pair(p.first, predictTokenSequence(p.second))
                }.asSequence()
    }

    fun predictFile(f: PsiFile): List<List<Double>>? {
        if (!tokenizerWrapper.willLexFile(f))
            return null

        modelWrapper.notify(f)
        return predictTokenSequence(tokenizerWrapper.lexFile(f))
    }


    private fun predictTokenSequence(lexed: Sequence<Sequence<String>>): List<List<Double>> {
        vocabularyWrapper

        vocabularyWrapper.setCheckpoint()
        val lineProbs: List<List<Double>>

        if (tokenizerWrapper.isPerLine) {
            lineProbs = lexed
                    .map { vocabularyWrapper.translateCodePiece(it) }
                    .map { it.toList() }
                    .map { predictTokens(it) }
                    .toList()
        } else {
            val lineLengths = ArrayList<Int>()

            val modeled = predictTokens(lexed
                    .map { vocabularyWrapper.translateCodePiece(it) }
                    .map { it.toList() }
                    .onEach { lineLengths.add(it.size) }
                    .flatMap { it.asSequence() }
                    .toList()
            )
            lineProbs = toLines(modeled, lineLengths)
        }

        vocabularyWrapper.restoreCheckpoint()
        return lineProbs
    }

    private fun predictTokens(tokens: List<Int>): List<Double> {
        if (selfTesting) modelWrapper.forget(tokens)

        val preds = toPredictions(modelWrapper.predict(tokens))
        val mrrs = (0 until tokens.size)
                .map { preds[it].indexOf(tokens[it]) }
                .map { toMRR(it) }

        if (selfTesting)
            modelWrapper.learn(tokens)

        return mrrs
    }


    private fun <K> toLines(modeled: List<K>, lineLengths: List<Int>): List<List<K>> {
        val perLine = ArrayList<List<K>>()
        var ix = 0

        lineLengths.indices.forEach { i ->
            val line = ArrayList<K>()
            for (j in 0 until lineLengths[i]) {
                line.add(modeled[ix++])
            }
            perLine.add(line)
        }

        return perLine
    }

    fun completeDirectory(file: PsiDirectory): Sequence<Pair<PsiFile, List<List<Completion>>>> {
        return tokenizerWrapper.lexDirectory(file)!!.asSequence()
                .map { p ->
                    modelWrapper.notify(p.first)
                    Pair(p.first, completeTokenSequence(p.second))
                }
    }

    fun completeFile(f: PsiFile): List<List<Completion>>? {
        if (!tokenizerWrapper.willLexFile(f)) return null
        modelWrapper.notify(f)
        return completeTokenSequence(tokenizerWrapper.lexFile(f))
    }

    private fun completeTokenSequence(lexed: Sequence<Sequence<String>>): List<List<Completion>> {
        val lineCompletions: List<List<Completion>>
        if (tokenizerWrapper.isPerLine) {
            lineCompletions = lexed
                    .map { vocabularyWrapper.translateCodePiece(it).asSequence() }
                    .map { line -> completeTokens(line) }
                    .asStream()
                    .collect(Collectors.toList())
        } else {
            val lineLengths = ArrayList<Int>()
            val commpletions = completeTokens(
                lexed
                        .map { vocabularyWrapper.translateCodePiece(it) }
                        .map { l -> l.toList() }
                        .onEach { l -> lineLengths.add(l.size) }
                        .flatMap { it.asSequence() }
            )
            lineCompletions = toLines(commpletions, lineLengths)
        }
        return lineCompletions
    }

    private fun completeTokens(tokens: Sequence<Int>): List<Completion> {
        val tokenss = tokens.asStream().collect(Collectors.toList())
        if (selfTesting) modelWrapper.forget(tokenss)
        val preds = modelWrapper.predict(tokenss)
        if (this.selfTesting) modelWrapper.learn(tokenss)
        return IntStream.range(0, preds.size)
                .mapToObj { i ->
                    val completions = preds[i].entries.stream()
                            .map { e -> Pair(e.key, PredictionWithConf.toProb(e.value, vocabularyWrapper.getVocabularySize())) }
                            .sorted { p1, p2 -> -p1.second.compareTo(p2.second) }
                            .limit(GLOBAL_PREDICTION_CUTOFF)
                            .collect(Collectors.toList())
                    Completion(tokenss[i], completions)
                }.collect(Collectors.toList())
    }


    private fun toProb(probWithConfs: List<PredictionWithConf>): List<Double> {
        return probWithConfs.map { PredictionWithConf.toProb(it, vocabularyWrapper.getVocabularySize()) }
    }

    private fun toPredictions(probConfs: List<Map<Int, PredictionWithConf>>): List<List<Int>> {
        return probConfs.map { toPredictions(it) }

    }

    private fun toPredictions(probWithConf: Map<Int, PredictionWithConf>): List<Int> {
        return probWithConf
                .map { Pair(it.key, PredictionWithConf.toProb(it.value, vocabularyWrapper.getVocabularySize())) }
                .sortedByDescending { it.second }
                .take(GLOBAL_PREDICTION_CUTOFF.toInt())
                .map { it.first }
    }

    companion object {

        private val INV_NEG_LOG_2 = -1.0 / ln(2.0)

        const val GLOBAL_PREDICTION_CUTOFF = 10L

        fun toEntropy(probability: Double): Double {
            return ln(probability) * INV_NEG_LOG_2
        }

        fun toMRR(ix: Int): Double {
            return if (ix >= 0) 1.0 / (ix + 1) else 0.0
        }
    }
}