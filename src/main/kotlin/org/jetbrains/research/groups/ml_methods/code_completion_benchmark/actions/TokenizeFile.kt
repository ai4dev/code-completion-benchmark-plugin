package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.actions

import com.intellij.ide.scratch.RootType
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.util.PathUtil
import com.intellij.openapi.command.WriteCommandAction.writeCommandAction
import com.intellij.ui.AppUIUtil

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.tokenization.tokenizers.JavaTokenizer

class TokenizeFile : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        JavaTokenizer().let { tokenizer ->
            val fileText = event.dataContext.getData("fileText") as String
            val tokenized = tokenizer.tokenizeLines(fileText)
            tryToOpenInScratch(project, concatLines(tokenized))
        }
    }

    private fun concatLines(lines: List<List<String>>): String {
        return with(lines) {
            forEach { lineSeq -> lineSeq.joinToString(" ")}
            joinToString("\n")
        }
    }

    private fun tryToOpenInScratch(project: Project, text: String): Boolean {
        val fileName = PathUtil.makeFileName("tokens", "txt")
        try {
            val computable = ThrowableComputable<NavigatablePsiElement, Exception> {
                val fileService = ScratchFileService.getInstance()
                val file = fileService.findFile(RootType.findById("scratches"), fileName, ScratchFileService.Option.create_new_always)

                fileService.scratchesMapping.setMapping(file, Language.ANY)
                val psiFile = PsiManager.getInstance(project).findFile(file)
                val document = (if (psiFile != null) PsiDocumentManager.getInstance(project).getDocument(psiFile) else null)
                    ?: return@ThrowableComputable null
                document.insertString(document.textLength, text)
                PsiDocumentManager.getInstance(project).commitDocument(document)
                psiFile!!
            }

            val psiElement = writeCommandAction(project)
                    .withName("Tokenizing file")
                    .compute(computable)

            if (psiElement != null) {
                AppUIUtil.invokeOnEdt(Runnable { psiElement.navigate(true) }, project.disposed)
                return true
            }
        } catch (e: Exception) {
            // ignore
        }

        return false
    }
}