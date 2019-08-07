package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.lang.collectors

import com.intellij.lang.ASTNode
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import com.intellij.util.containers.TreeTraversal

class JavaASTCollector private constructor() {
    val elements = ArrayList<ASTNode>()

    //TODO: filter whitespaces, punctuation marks etc
    private fun collect(node: ASTNode) {
        SyntaxTraverser
                .astTraverser(node)
                .traverse(TreeTraversal.LEAVES_DFS)
                .filter {
                    it.text.isNotEmpty() &&
                            it.elementType != JavaTokenType.C_STYLE_COMMENT &&
                            it.elementType != JavaTokenType.END_OF_LINE_COMMENT
                }
                .onEach { elements.add(it) }
    }

    companion object {
        fun getElements(file: PsiFile): ArrayList<ASTNode> {
            val root = file.node
            JavaASTCollector().let { collector ->
                collector.collect(root)
                return collector.elements
            }
        }
    }
}