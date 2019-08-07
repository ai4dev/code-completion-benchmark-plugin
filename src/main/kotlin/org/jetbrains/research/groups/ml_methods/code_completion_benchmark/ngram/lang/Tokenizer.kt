package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.lang

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile

interface Tokenizer {
    fun tokenizeFile(file: PsiFile): Sequence<Sequence<String>>

    fun itemsToLines(items: List<ASTNode>): List<List<String>>
}

