package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.tokenization.tokenizers.JavaTokenizer
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.tokenization.wrappers.TokenizerWrapper
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.builders.VocabularyBuilder
import java.io.File

class BuildVocabularyAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val doc = (event.dataContext.getData("fileEditor") as TextEditor).editor.document
        val file = FileDocumentManager.getInstance().getFile(doc) ?: return
        val vocab = VocabularyBuilder.build(TokenizerWrapper(JavaTokenizer(), true), File(file.path))
        VocabularyBuilder.write(vocab, File("/Users/username/Desktop/vocab.txt"))
    }
}