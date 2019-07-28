package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.completion.ngram

import com.intellij.lang.ASTNode
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.FileType

interface NGramElementProvider {

    fun getSupportedFileTypes(): Set<FileType>

    fun getElementRepresentation(element: ASTNode): String

    fun shouldIndex(element: ASTNode, content: CharSequence): Boolean

    companion object {

        private val EP_NAME = ExtensionPointName.create<NGramElementProvider>(
            "org.jetbrains.research.groups.ml_methods.code_completion_benchmark.completion.ngram.ngramElementProvider"
        )

        fun getSupportedFileTypes(): Set<FileType>  {
            return ExtensionPointName<NGramElementProvider>(EP_NAME.name)
                    .extensionList
                    .flatMap { it.getSupportedFileTypes() }
                    .toSet()
        }

        fun getElementRepresentation(element: ASTNode): String {
            ExtensionPointName<NGramElementProvider>(EP_NAME.name).extensionList.forEach {
                val representation = it.getElementRepresentation(element)
                if (representation.isNotEmpty()) {
                    return@getElementRepresentation representation
                }
            }
            throw IllegalStateException("no suitable representation found")
        }

        fun shouldIndex(element: ASTNode, content: CharSequence): Boolean {
            return ExtensionPointName<NGramElementProvider>(EP_NAME.name)
                    .extensionList
                    .all { it.shouldIndex(element, content) }
        }
    }
}

abstract class AbstractNGramElementProvider: NGramElementProvider {
    override fun getElementRepresentation(element: ASTNode): String {
        return element.text
    }
}