package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.collectors

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import com.intellij.util.containers.TreeTraversal

class JavaASTCollector private constructor() {
    val elements = ArrayList<ASTNode>()

    //TODO: filter whitespaces???
    private fun collect(node: ASTNode) {
        SyntaxTraverser
                .astTraverser(node)
                .traverse(TreeTraversal.LEAVES_DFS)
                .filter { it.text.isNotEmpty() }
                .onEach { elements.add(it) }
    }

    companion object {
        fun getElements(file: PsiFile): ArrayList<ASTNode> {
            val root = file.node
            JavaASTCollector().let { collector ->
                collector.collect(root)
                println(collector.elements)
                return collector.elements
            }
        }
    }
}