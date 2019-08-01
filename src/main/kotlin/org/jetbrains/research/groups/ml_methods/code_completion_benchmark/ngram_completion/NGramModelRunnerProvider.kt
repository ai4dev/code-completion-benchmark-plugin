package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram_completion

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.toolkit.providers.ModelRunnerProvider

class NGramModelRunnerProvider : ModelRunnerProvider {

    override fun getModelRunners(): Array<Class<*>> {
        return arrayOf(NGramModelRunner::class.java)
    }
}