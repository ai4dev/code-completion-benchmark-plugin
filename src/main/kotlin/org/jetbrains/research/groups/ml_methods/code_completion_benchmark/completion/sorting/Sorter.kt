package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.completion.sorting

import com.intellij.codeInsight.completion.CompletionFinalSorter
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.Pair
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.completion.ngram.NGram
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.completion.ngram.components.ModelComponent

@Suppress("DEPRECATION")
class CompletionSorterFactory : CompletionFinalSorter.Factory {
    override fun newSorter() = Sorter()
}

class Sorter : CompletionFinalSorter() {

    private fun rankItems(items: MutableIterable<LookupElement>, nGram: NGram): MutableIterable<LookupElement> {
        //TODO: implement ranking
        val completions = ModelComponent.getNGramSuggestions(nGram)
        return items
    }

    override fun sort(items: MutableIterable<LookupElement>, parameters: CompletionParameters): MutableIterable<LookupElement> {
        val position = parameters.originalPosition ?: parameters.position
        val nGram = NGram.getNGramForElement(position)
        return rankItems(items, nGram)
    }

    override fun getRelevanceObjects(elements: MutableIterable<LookupElement>): Map<LookupElement, MutableList<Pair<String, Any>>> {
        return elements.associateWith { listOf(Pair.create("", "" as Any)).toMutableList() }
    }
}