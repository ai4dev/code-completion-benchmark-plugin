package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.completion.ngram

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor

class ASTCollector : PsiRecursiveElementVisitor() {
    val elements = ArrayList<ASTNode>()

    private fun dfs(node: ASTNode) {
        var root = node.firstChildNode
        root?.let {
            elements.add(node)
        }
        while (root != null) {
            dfs(root)
            root = root.treeNext
        }
    }

    companion object {
        fun getElements(file: PsiFile): ArrayList<ASTNode> {
            val root = file.node
            val astCollector = ASTCollector()
            astCollector.dfs(root)
            return astCollector.elements
        }
    }
}