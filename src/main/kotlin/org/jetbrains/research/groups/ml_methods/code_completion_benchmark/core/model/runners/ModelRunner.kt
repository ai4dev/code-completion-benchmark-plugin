package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.runners

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.completion.ngram.NGram
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.wrappers.TokenizerWrapper
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.Completion
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.ConfPrediction
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.Model
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.Vocabulary

import java.io.File
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.streams.asStream


class ModelRunner(
        val model: Model,
        val tokenizerWrapper: TokenizerWrapper,
        val vocabulary: Vocabulary
) {

    var selfTesting = false

    //TODO: remove after testing
    private var learnStats = LongArray(2)
    private var modelStats = LongArray(2)


    private var ent = 0.0
    private var mrr = 0.0

    fun copyForModel(model: Model): ModelRunner {
        return ModelRunner(model, tokenizerWrapper, vocabulary)
    }

    fun learnDirectory(file: PsiDirectory) {
        learnStats = longArrayOf(0, -System.currentTimeMillis())
        tokenizerWrapper.lexDirectory(file)!!
                .forEach { p ->
                    model.notify(p.first)
                    learnTokens(p.second)
                }
        if (learnStats[0] > LEARN_PRINT_INTERVAL && learnStats[1] != 0L) {
            println(
                "Counting complete: ${learnStats[0]}M tokens processed " +
                        "in ${(System.currentTimeMillis() + learnStats[1]) / 1000}s\n"
            )
        }
    }

    fun learnFile(f: PsiFile) {
        if (!tokenizerWrapper.willLexFile(f))
            return

        model.notify(f)
        learnTokens(tokenizerWrapper.lexFile(f))
    }

    fun learnTokens(lexed: Sequence<Sequence<String>>) {
        if (tokenizerWrapper.isPerLine) {
            lexed
                    .map { vocabulary.toIndices(it) }
                    .map { it.onEach { logLearningProgress() } }
                    .map { it.toList() }
                    .forEach { model.learn(it) }
        } else {
            model.learn(lexed
                    .map { it.onEach { logLearningProgress() } }
                    .flatMap { vocabulary.toIndices(it) }
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

        model.notify(f)
        forgetTokens(tokenizerWrapper.lexFile(f))
    }

    fun forgetTokens(lexed: Sequence<Sequence<String>>) {
        if (tokenizerWrapper.isPerLine) {
            lexed.map { vocabulary.toIndices(it) }
                    .map { it.toList() }
                    .forEach { model.forget(it) }
        } else {
            model.forget(
                lexed
                        .flatMap { vocabulary.toIndices(it) }
                        .toList()
            )
        }
    }

    fun modelDirectory(file: PsiDirectory): Sequence<Pair<PsiFile, List<List<Double>>>> {
        modelStats = longArrayOf(0, -System.currentTimeMillis())
        ent = 0.0
        return tokenizerWrapper.lexDirectory(file)!!
                .map { p ->
                    model.notify(p.first)
                    Pair(p.first, modelTokens(p.second))
                }.asSequence()
    }

    fun modelFile(f: PsiFile): List<List<Double>>? {
        if (!tokenizerWrapper.willLexFile(f))
            return null

        model.notify(f)
        return modelTokens(tokenizerWrapper.lexFile(f))
    }

    fun modelTokens(lexed: Sequence<Sequence<String>>): List<List<Double>> {
        vocabulary.setCheckpoint()
        val lineProbs: List<List<Double>>

        if (tokenizerWrapper.isPerLine) {
            lineProbs = lexed
                    .map { vocabulary.toIndices(it) }
                    .map { it.toList() }
                    .map { modelSequence(it) }
                    .onEach { logModelingProgress(it) }
                    .toList()
        } else {
            val lineLengths = ArrayList<Int>()

            val modeled = modelSequence(
                lexed
                        .map { vocabulary.toIndices(it) }
                        .map { it.toList() }
                        .onEach { lineLengths.add(it.size) }
                        .flatMap { it.asSequence() }
                        .toList()
            )
            lineProbs = toLines(modeled, lineLengths)

            logModelingProgress(modeled)
        }

        vocabulary.restoreCheckpoint()
        return lineProbs
    }

    private fun modelSequence(tokens: List<Int>): List<Double> {
        if (selfTesting) model.forget(tokens)

        val entropies = model.model(tokens).stream()
                .map { toProb(it) }
                .map { toEntropy(it) }
                .collect(Collectors.toList())

        if (selfTesting)
            model.learn(tokens)

        return entropies
    }

    fun predict(file: PsiDirectory): Sequence<Pair<PsiFile, List<List<Double>>>> {
        modelStats = longArrayOf(0, -System.currentTimeMillis())
        mrr = 0.0

        return tokenizerWrapper.lexDirectory(file)!!
                .map { p ->
                    model.notify(p.first)
                    Pair(p.first, predictTokens(p.second))
                }.asSequence()
    }

    fun predictFile(f: PsiFile): List<List<Double>>? {
        if (!tokenizerWrapper.willLexFile(f))
            return null

        model.notify(f)
        return predictTokens(tokenizerWrapper.lexFile(f))
    }


    fun predictTokens(lexed: Sequence<Sequence<String>>): List<List<Double>> {
        vocabulary.setCheckpoint()
        val lineProbs: List<List<Double>>

        if (tokenizerWrapper.isPerLine) {
            lineProbs = lexed
                    .map { vocabulary.toIndices(it) }
                    .map { it.toList() }
                    .map { predictSequence(it) }
                    .onEach { logPredictionProgress(it) }
                    .toList()
        } else {
            val lineLengths = ArrayList<Int>()

            val modeled = predictSequence(lexed
                    .map { vocabulary.toIndices(it) }
                    .map { it.toList() }
                    .onEach { lineLengths.add(it.size) }
                    .flatMap { it.asSequence() }
                    .toList()
            )
            lineProbs = toLines(modeled, lineLengths)

            logPredictionProgress(modeled)
        }

        vocabulary.restoreCheckpoint()
        return lineProbs
    }

    protected fun predictSequence(tokens: List<Int>): List<Double> {
        if (selfTesting) model.forget(tokens)

        val preds = toPredictions(model.predict(tokens))
        val mrrs = (0 until tokens.size)
                .map { preds[it].indexOf(tokens[it]) }
                .map { toMRR(it) }

        if (selfTesting)
            model.learn(tokens)

        return mrrs
    }

    fun toProb(probConfs: List<ConfPrediction>): List<Double> {
        return probConfs.map { toProb(it) }
    }

    fun toProb(probConf: ConfPrediction): Double {
        val prob = probConf.probability
        val conf = probConf.confidence
        return prob * conf + (1 - conf) / vocabulary.size()
    }

    fun toPredictions(probConfs: List<Map<Int, ConfPrediction>>): List<List<Int>> {
        return probConfs.map { toPredictions(it) }

    }

    fun toPredictions(probConf: Map<Int, ConfPrediction>): List<Int> {
        return probConf
                .map { Pair(it.key, toProb(it.value)) }
                .sortedByDescending { it.second }
                .take(GLOBAL_PREDICTION_CUTOFF.toInt())
                .map { it.first }
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

    fun getStats(fileProbs: Map<File, List<List<Double>>>): DoubleSummaryStatistics {
        return getStats(fileProbs.map { Pair(it.key, it.value) }.asSequence())
    }

    fun getStats(fileProbs: Sequence<Pair<File, List<List<Double>>>>): DoubleSummaryStatistics {
        return getFileStats(fileProbs.map { p -> p.second })
    }

    fun getStats(fileProbs: List<List<Double>>): DoubleSummaryStatistics {
        return getFileStats(sequenceOf(fileProbs))
    }

    private fun getFileStats(fileProbs: Sequence<List<List<Double>>>): DoubleSummaryStatistics {
        return if (tokenizerWrapper.isPerLine) {
            fileProbs
                    .flatMap { it.asSequence() }
                    .flatMap { it.asSequence().drop(1) }
                    .asStream()
                    .mapToDouble { it }
                    .summaryStatistics()
        } else {
            fileProbs
                    .flatMap {
                        it.asSequence()
                                .flatMap { element -> element.asSequence() }
                                .drop(1)
                    }
                    .asStream()
                    .mapToDouble { it }
                    .summaryStatistics()
        }
    }

    //TODO: remove after testing
    private fun logLearningProgress() {
        if (++learnStats[0] % LEARN_PRINT_INTERVAL == 0L && learnStats[1] != 0L) {
            println(
                "Counting: ${(learnStats[0] / 1e6).roundToInt()} tokens processed " +
                        "in ${(System.currentTimeMillis() + learnStats[1]) / 1000}"
            )
        }
    }

    private fun logModelingProgress(modeled: List<Double>) {
        val stats = modeled.stream().skip(1)
                .mapToDouble { it.toDouble() }.summaryStatistics()
        val prevCount = modelStats[0]
        modelStats[0] += stats.count
        ent += stats.sum

        if (modelStats[0] / MODEL_PRINT_INTERVAL > prevCount / MODEL_PRINT_INTERVAL && modelStats[1] != 0L) {
            println(
                "Modeling: ${(modelStats[0] / 1e3).roundToInt()} tokens processed " +
                        "in ${(System.currentTimeMillis() + modelStats[1]) / 1000}, " +
                        "avg. entropy: ${ent / modelStats[0]}\n")
        }
    }

    private fun logPredictionProgress(modeled: List<Double>) {
        val stats = modeled
                .stream()
                .skip(1)
                .mapToDouble { it.toDouble() }
                .summaryStatistics()

        val prevCount = modelStats[0]
        modelStats[0] += stats.count
        mrr += stats.sum

        if (modelStats[0] / MODEL_PRINT_INTERVAL > prevCount / MODEL_PRINT_INTERVAL && modelStats[1] != 0L) {
            println(
                "Predicting: ${(modelStats[0] / 1e3).roundToInt()} tokens processed " +
                        "in ${(System.currentTimeMillis() + modelStats[1]) / 1000}, " +
                        "avg. MRR: ${mrr / modelStats[0]}\n"
            )
        }
    }

    fun completeDirectory(file: PsiDirectory): Sequence<Pair<PsiFile, List<List<Completion>>>> {
        this.modelStats = longArrayOf(0, -System.currentTimeMillis())
        this.mrr = 0.0
        return tokenizerWrapper.lexDirectory(file)!!.asSequence()
                .map { p ->
                    model.notify(p.first)
                    Pair(p.first, completeTokens(p.second))
                }
    }

    fun completeFile(f: PsiFile): List<List<Completion>>? {
        if (!tokenizerWrapper.willLexFile(f)) return null
        model.notify(f)
        return completeTokens(tokenizerWrapper.lexFile(f))
    }

    fun completeTokens(lexed: Sequence<Sequence<String>>): List<List<Completion>> {
        val lineCompletions: List<List<Completion>>
        if (tokenizerWrapper.isPerLine) {
            lineCompletions = lexed
                    .map { vocabulary.toIndices(it) }
                    .map { line -> completeSequence(line) }
                    .asStream()
                    .collect(Collectors.toList())
        } else {
            val lineLengths = ArrayList<Int>()
            val commpletions = completeSequence(
                lexed
                        .map { vocabulary.toIndices(it).asStream() }
                        .map { l -> l.collect(Collectors.toList()) }
                        .onEach { l -> lineLengths.add(l.size) }
                        .flatMap { it.asSequence() }
            )
            lineCompletions = toLines(commpletions, lineLengths)
        }
        return lineCompletions
    }

    protected fun completeSequence(tokens: Sequence<Int>): List<Completion> {
        val tokenss = tokens.asStream().collect(Collectors.toList())
        if (selfTesting) model.forget(tokenss)
        val preds = model.predict(tokenss)
        if (this.selfTesting) model.learn(tokenss)
        return IntStream.range(0, preds.size)
                .mapToObj { i ->
                    val completions = preds[i].entries.stream()
                            .map { e -> Pair(e.key, toProb(e.value)) }
                            .sorted { p1, p2 -> -p1.second.compareTo(p2.second) }
                            .limit(GLOBAL_PREDICTION_CUTOFF)
                            .collect(Collectors.toList())
                    Completion(tokenss[i], completions)
                }.collect(Collectors.toList())
    }

    fun completeNGram(nGram: NGram): List<Pair<Int, Double>> {
        val elements = nGram.elements.asSequence()
        val tokens = vocabulary.toIndices(elements)
        val completions = completeSequence(tokens).lastOrNull() ?: return emptyList()
        return completions.predictions
    }

    companion object {
        //TODO: remove after testing
        private const val LEARN_PRINT_INTERVAL: Long = 1000000
        private const val MODEL_PRINT_INTERVAL = 100000

        private val INV_NEG_LOG_2 = -1.0 / ln(2.0)
        const val DEFAULT_NGRAM_ORDER = 6

        const val GLOBAL_PREDICTION_CUTOFF = 10L

        fun toEntropy(probability: Double): Double {
            return ln(probability) * INV_NEG_LOG_2
        }

        fun toMRR(ix: Int): Double {
            return if (ix >= 0) 1.0 / (ix + 1) else 0.0
        }
    }
}