package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.mix

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.Model
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.ConfPrediction
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.dynamic.CacheModel
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.ngrams.JMModel
import java.io.File
import kotlin.math.max

class InverseMixModel(
        model1: Model = JMModel(),
        model2: Model = CacheModel()
) : MixedModel(model1, model2) {

    override fun mix(
            input: List<Int>,
            index: Int,
            res1: ConfPrediction,
            res2: ConfPrediction
    ): ConfPrediction = when {
            res1.confidence == 0.0 && res2.confidence == 0.0 -> ConfPrediction(0.0, 0.0)
            res2.confidence == 0.0 -> res1
            res1.confidence == 0.0 -> res2
            else -> {
                val lNorm = if (res1.confidence > 0.999) 1000.0 else 1.0 / (1 - res1.confidence)
                val rNorm = if (res2.confidence > 0.999) 1000.0 else 1.0 / (1 - res2.confidence)

                val probability = (res1.probability * lNorm + res2.probability * rNorm) / (lNorm + rNorm)
                val confidence = max(res1.confidence, res2.confidence)

                ConfPrediction(probability, confidence)
            }
    }

    override fun load(directory: File): MixedModel {
        val leftModel = left.load(getLeftDirectoryName(directory))
        val rightModel = right.load(getRightDirectoryName(directory))

        return InverseMixModel(leftModel, rightModel)
    }

    companion object {
        fun load(directory: File) = InverseMixModel().load(directory)
        fun save(directory: File, model: InverseMixModel) = model.save(directory)
    }
}