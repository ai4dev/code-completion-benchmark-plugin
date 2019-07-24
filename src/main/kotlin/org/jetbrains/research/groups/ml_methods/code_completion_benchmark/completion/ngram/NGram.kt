package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.completion.ngram

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

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

        fun getNGramForElement(element: PsiElement): NGram {
            val elements = ASTCollector.getElements(element.containingFile ?: return INVALID_NGRAM)
            val index = elements.indexOf(element.parent.node)
            if (index == -1 || index < DEFAULT_NGRAM_ORDER) {
                return INVALID_NGRAM
            }
            val nGramElements = ArrayList<String>()
            (1..DEFAULT_NGRAM_ORDER).forEach { i ->
                nGramElements.add(getElementRepresentation(elements[index - i]))
            }
            return NGram(nGramElements)
        }

        fun getElementRepresentation(element: ASTNode): String {
            val file = element.psi.containingFile
            return file.text.substring(element.textRange.startOffset, element.textRange.endOffset)
        }
    }
}