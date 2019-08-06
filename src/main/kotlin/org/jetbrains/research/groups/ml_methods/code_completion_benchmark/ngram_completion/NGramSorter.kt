package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram_completion

import com.intellij.codeInsight.completion.CompletionFinalSorter
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.Pair
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.ngram.NGram
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.toolkit.services.ModelRunnerRegistrar
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.toolkit.sorting.Sorter

@Suppress("DEPRECATION")
class CompletionSorterFactory : CompletionFinalSorter.Factory {
    override fun newSorter() = NGramSorter()
}

class NGramSorter : Sorter() {
    override fun rankCompletions(
            completions: MutableIterable<LookupElement>,
            parameters: CompletionParameters
    ): MutableIterable<LookupElement>? {
        val nGram = NGram.getNGramForElement(getPsiElementByParameters(parameters)).elements

        val modelCompletions = modelService
                ?.getTopNCodeSuggestions(nGram)
                ?.map { it.prediction } ?: return null
        val stringCompletions = completions.map { Pair(it, it.lookupString) }

        val result = mutableListOf<LookupElement>()
        modelCompletions.forEach { comp ->
            stringCompletions.find { it.second == comp }?.let {
                result.add(it.first)
            }
        }
        return if (result.isNullOrEmpty()) completions else result
    }

    companion object {
        private val RUNNER_ID = NGramModelRunner::class.java.name
        private val modelService = ModelRunnerRegistrar.getInstance().getRunnerById(RUNNER_ID)
    }
}
