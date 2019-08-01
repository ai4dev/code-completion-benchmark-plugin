package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.ngram

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.collectors.JavaASTCollector

data class NGram(val elements: List<String>) {

    fun appendToken(token: String): NGram {
        return NGram(listOf(*elements.toTypedArray(), token))
    }

    fun dropHeadToken(): NGram {
        return NGram(elements.drop(1))
    }

    companion object {

        const val DEFAULT_NGRAM_ORDER = 6
        val INVALID_NGRAM = NGram(emptyList())

        fun parseFile(psiFile: PsiFile, content: CharSequence): HashMap<NGram, Int> {
            val result = HashMap<NGram, Int>()
            val elements = JavaASTCollector.getElements(psiFile)
            if (elements.size > DEFAULT_NGRAM_ORDER) {
                for (i in DEFAULT_NGRAM_ORDER until elements.size) {
                    if (NGramElementProvider.shouldIndex(elements[i], content)) {
                        val nGramElements = ArrayList<String>()
                        (1..DEFAULT_NGRAM_ORDER).forEach { j ->
                            nGramElements.add(NGramElementProvider.getElementRepresentation(elements[i - j]))
                        }
                        nGramElements.add(elements[i].text)
                        (0..DEFAULT_NGRAM_ORDER).forEach { j ->
                            val ngram = NGram(nGramElements.subList(j, nGramElements.size))
                            val oldValue = result.putIfAbsent(ngram, 1)
                            oldValue?.let {
                                result[ngram] = oldValue + 1
                            }
                        }
                    }
                }
            }
            return result
        }

        fun getNGramForElement(element: PsiElement): NGram {
            val elements = JavaASTCollector.getElements(element.containingFile ?: return INVALID_NGRAM)
            val index = elements.indexOf(element.node)
            if (index == -1 || index < DEFAULT_NGRAM_ORDER) {
                return INVALID_NGRAM
            }
            val nGramElements = ArrayList<String>()
            (1..DEFAULT_NGRAM_ORDER).forEach { i ->
                nGramElements.add(NGramElementProvider.getElementRepresentation(elements[index - i]))
            }
            return NGram(nGramElements)
        }
    }
}