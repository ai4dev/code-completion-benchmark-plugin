package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.modeling.base

data class ConfPrediction(val probability: Double, val confidence: Double) {
    constructor(pair: Pair<Double, Double>) : this(pair.first, pair.second)
}