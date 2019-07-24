package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.actions.utils.ScratchUtils
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.tokenizers.JavaTokenizer

class TokenizeFileAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        JavaTokenizer().let { tokenizer ->
            val fileText = event.dataContext.getData("fileText") as String
            val tokenized = tokenizer.tokenizeLines(fileText)
            ScratchUtils.tryToOpenInScratch(project, ScratchUtils.concatLines(tokenized))
        }
    }
}