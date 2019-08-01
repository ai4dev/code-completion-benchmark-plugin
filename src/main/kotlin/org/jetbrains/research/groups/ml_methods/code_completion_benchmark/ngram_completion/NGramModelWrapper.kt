package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram_completion

import com.intellij.psi.PsiFile
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.PredictionWithConf
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.mix.InverseMixModel
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.toolkit.model.AbstractModelWrapper

class NGramModelWrapper : AbstractModelWrapper<InverseMixModel>() {
    override val model: InverseMixModel = InverseMixModel()

    fun notify(next: PsiFile) {
        model.notify(next)
    }

    fun learn(input: List<Int>) {
        model.learn(input)
    }

    fun forget(input: List<Int>) {
        model.forget(input)
    }

    fun model(input: List<Int>): List<PredictionWithConf> {
        return model.model(input)
    }

    fun predictToken(input: List<Int>, index: Int): Map<Int, PredictionWithConf> {
        return model.predictToken(input, index)
    }

    fun predict(input: List<Int>): List<Map<Int, PredictionWithConf>>  {
        return model.predict(input)
    }
}