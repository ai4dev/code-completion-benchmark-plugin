package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.tokenization.tokenizers.JavaTokenizer
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.tokenization.wrappers.TokenizerWrapper
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.builders.VocabularyBuilder
import java.io.File
import java.nio.file.Paths

class BuildVocabularyAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val doc = (event.dataContext.getData("fileEditor") as? TextEditor)?.editor?.document ?: return
        val file = FileDocumentManager.getInstance().getFile(doc) ?: return
        val vocab = VocabularyBuilder.build(TokenizerWrapper(JavaTokenizer(), true), File(file.path))
        VocabularyBuilder.write(vocab, File(vocabPath.toString()))
    }

    companion object {
        private const val PLUGIN_NAME = "code-completion-benchmark"
        private val vocabPath = Paths.get(PathManager.getPluginsPath(), PLUGIN_NAME, "vocab.txt")
    }
}