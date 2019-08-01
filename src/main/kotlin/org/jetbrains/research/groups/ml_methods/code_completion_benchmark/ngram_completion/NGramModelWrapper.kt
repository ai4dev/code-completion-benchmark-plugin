package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram_completion

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.mix.InverseMixModel
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.toolkit.model.ModelWrapper

class NGramModelWrapper : ModelWrapper<InverseMixModel> {
    override val model: InverseMixModel = InverseMixModel()
}