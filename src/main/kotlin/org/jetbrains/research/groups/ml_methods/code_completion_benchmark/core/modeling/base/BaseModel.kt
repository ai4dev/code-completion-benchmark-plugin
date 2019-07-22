package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.modeling.base

abstract class BaseModel : Model {

    override var dynamic = false
        set(value) {
            field = value
            wasDynamic = value
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

    override fun learn(input: List<Int>) {
        (0 until input.size).forEach { learnToken(input, it) }
    }

    override fun forget(input: List<Int>) {
        (0 until input.size).forEach { forgetToken(input, it) }
    }

    override fun getConfidence(input: List<Int>, index: Int): Double {
        pauseDynamic()

        val confidence = predictAtIndex(input, index)
                .map { it.value.first }
                .sortedByDescending { it }
                .take(1)
                .sum()

        unPauseDynamic()
        return confidence
    }

    override fun model(input: List<Int>): List<Pair<Double, Double>> {
        return (0 until input.size)
                .map { modelToken(input, it) }
    }

    override fun modelToken(input: List<Int>, index: Int): Pair<Double, Double> {
        return modelAtIndex(input, index)
                .also {
                    if (dynamic) learnToken(input, index)
                }
    }

    override fun predict(input: List<Int>): List<Map<Int, Pair<Double, Double>>> {
        return (0 until input.size)
                .map { predictToken(input, it) }
    }

    abstract fun modelAtIndex(input: List<Int>, index: Int): Pair<Double, Double>

    override fun predictToken(input: List<Int>, index: Int): Map<Int, Pair<Double, Double>> {
        val temp = dynamic
        dynamic = false

        val predictions = predictAtIndex(input, index)

        dynamic = temp
        if (dynamic) learnToken(input, index)

        return predictions
    }

    abstract fun predictAtIndex(input: List<Int>?, index: Int): Map<Int, Pair<Double, Double>>

}