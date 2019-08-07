package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.actions.utils.ScratchUtils
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.lang.tokenizers.JavaTokenizer

class TokenizeFileAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(LangDataKeys.PSI_FILE) ?: return

        JavaTokenizer().let { tokenizer ->
            val tokenized = tokenizer.tokenizeFile(file)
            ScratchUtils.tryToOpenInScratch(project, ScratchUtils.concatLines(tokenized))
        }
    }
}