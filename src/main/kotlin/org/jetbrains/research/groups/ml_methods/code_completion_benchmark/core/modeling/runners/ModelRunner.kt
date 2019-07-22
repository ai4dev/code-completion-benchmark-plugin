package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.modeling.runners

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.modeling.base.Model
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.tokenization.wrappers.TokenizerWrapper
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.Vocabulary
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.stream.Collectors
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.streams.asStream

class ModelRunner(val model: Model, val tokenizerWrapper: TokenizerWrapper, val vocabulary: Vocabulary) {

    var selfTesting = false

    //TODO: remove after testing
    private var learnStats = LongArray(2)
    private var modelStats = LongArray(2)


    private var ent = 0.0
    private var mrr = 0.0

    fun copyForModel(model: Model): ModelRunner {
        return ModelRunner(model, tokenizerWrapper, vocabulary)
    }

    fun learnDirectory(file: File) {
        learnStats = longArrayOf(0, -System.currentTimeMillis())
        tokenizerWrapper.lexDirectory(file)!!
                .forEach { p ->
                    model.notify(p.first)
                    learnTokens(p.second)
                }
        if (learnStats[0] > LEARN_PRINT_INTERVAL && learnStats[1] != 0L) {
            println(
                "Counting complete: ${learnStats[0]} tokens processed " +
                        "in ${(System.currentTimeMillis() + learnStats[1]) / 1000}s\n"
            )
        }
    }

    fun learnFile(f: File) {
        if (!tokenizerWrapper.willLexFile(f))
            return

        model.notify(f)
        learnTokens(tokenizerWrapper.lexFile(f))
    }

    fun learnContent(content: String) {
        learnTokens(tokenizerWrapper.lexText(content))
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

    fun forgetDirectory(file: File) {
        try {
            Files.walk(file.toPath())
                    .map { it.toFile() }
                    .filter { it.isFile }
                    .forEach { forgetFile(it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun forgetFile(f: File) {
        if (!tokenizerWrapper.willLexFile(f))
            return

        model.notify(f)
        forgetTokens(tokenizerWrapper.lexFile(f))
    }

    fun forgetContent(content: String) {
        forgetTokens(tokenizerWrapper.lexText(content))
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

    fun modelDirectory(file: File): Sequence<Pair<File, List<List<Double>>>> {
        modelStats = longArrayOf(0, -System.currentTimeMillis())
        ent = 0.0
        return tokenizerWrapper.lexDirectory(file)!!
                .map { p ->
                    model.notify(p.first)
                    Pair(p.first, modelTokens(p.second))
                }
    }

    fun modelFile(f: File): List<List<Double>>? {
        if (!tokenizerWrapper.willLexFile(f))
            return null

        model.notify(f)
        return modelTokens(tokenizerWrapper.lexFile(f))
    }

    fun modelContent(content: String): List<List<Double>> {
        return modelTokens(tokenizerWrapper.lexText(content))
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

    fun predict(file: File): Sequence<Pair<File, List<List<Double>>>> {
        modelStats = longArrayOf(0, -System.currentTimeMillis())
        mrr = 0.0

        return tokenizerWrapper.lexDirectory(file)!!
                .map { p ->
                    model.notify(p.first)
                    Pair(p.first, predictTokens(p.second))
                }
    }

    fun predictFile(f: File): List<List<Double>>? {
        if (!tokenizerWrapper.willLexFile(f))
            return null

        model.notify(f)
        return predictTokens(tokenizerWrapper.lexFile(f))
    }

    fun predictContent(content: String): List<List<Double>> {
        return predictTokens(tokenizerWrapper.lexText(content))
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

    fun toProb(probConfs: List<Pair<Double, Double>>): List<Double> {
        return probConfs.map { toProb(it) }
    }

    fun toProb(probConf: Pair<Double, Double>): Double {
        val prob = probConf.first
        val conf = probConf.second
        return prob * conf + (1 - conf) / vocabulary.size()
    }

    fun toPredictions(probConfs: List<Map<Int, Pair<Double, Double>>>): List<List<Int>> {
        return probConfs.map { toPredictions(it) }

    }

    fun toPredictions(probConf: Map<Int, Pair<Double, Double>>): List<Int> {
        return probConf
                .map { Pair(it.key, toProb(it.value)) }
                .sortedByDescending { it.second }
                .take(predictionCutOff)
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

    companion object {
        //TODO: remove after testing
        private const val LEARN_PRINT_INTERVAL: Long = 1000000
        private const val MODEL_PRINT_INTERVAL = 100000

        private val INV_NEG_LOG_2 = -1.0 / ln(2.0)
        const val DEFAULT_NGRAM_ORDER = 6

        var predictionCutOff = 10

        fun toEntropy(probability: Double): Double {
            return ln(probability) * INV_NEG_LOG_2
        }

        fun toMRR(ix: Int): Double {
            return if (ix >= 0) 1.0 / (ix + 1) else 0.0
        }
    }
}