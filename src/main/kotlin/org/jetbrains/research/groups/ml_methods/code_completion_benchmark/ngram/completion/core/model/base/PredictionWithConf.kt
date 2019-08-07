package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.completion.core.model.base


data class PredictionWithConf(val probability: Double, val confidence: Double) {
    constructor(pair: Pair<Double, Double>) : this(pair.first, pair.second)

    companion object {
        fun toProb(probWithConf: PredictionWithConf, vocabSize: Int): Double {
            val prob = probWithConf.probability
            val conf = probWithConf.confidence
            return prob * conf + (1 - conf) / vocabSize
        }
    }
}