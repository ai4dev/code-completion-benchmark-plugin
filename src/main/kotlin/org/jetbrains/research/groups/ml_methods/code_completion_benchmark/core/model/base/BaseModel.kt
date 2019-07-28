package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base

abstract class BaseModel : Model {

    override var dynamic = false
        set(value) {
            wasDynamic = value
            field = value
        }

    private var wasDynamic = false
    private var dynamicDepth = 0

    override fun pauseDynamic() {
        dynamicDepth++
        dynamic = false
    }

    override fun unPauseDynamic() {
        if (wasDynamic && dynamicDepth > 0 && --dynamicDepth == 0) {
            dynamic = true
        }
    }

    override fun getConfidence(input: List<Int>, index: Int): Double {
        pauseDynamic()

        val confidence = predictAtIndex(input, index)
                .map { it.value.probability }
                .sortedByDescending { it }
                .take(1)
                .sum()

        unPauseDynamic()
        return confidence
    }

    override fun modelToken(input: List<Int>, index: Int): ConfPrediction {
        val modeled = modelAtIndex(input, index)
        if (dynamic) learnToken(input, index)
        return modeled
    }

    abstract fun modelAtIndex(input: List<Int>, index: Int): ConfPrediction

    override fun predictToken(input: List<Int>, index: Int): Map<Int, ConfPrediction> {
        val temp = dynamic
        dynamic = false

        val predictions = predictAtIndex(input, index)

        dynamic = temp
        if (dynamic) modelToken(input, index)
        return predictions
    }

    abstract fun predictAtIndex(input: List<Int>, index: Int): Map<Int, ConfPrediction>
}