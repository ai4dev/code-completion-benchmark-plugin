package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.tokenizers.JavaTokenizer
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.wrappers.TokenizerWrapper
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.builders.VocabularyBuilder
import java.io.File
import java.nio.file.Paths

class BuildVocabularyAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val proj = event.project ?: return
        val projDir = proj.guessProjectDir() ?: return
        val psiDir = PsiManager.getInstance(proj).findDirectory(projDir) ?: return
        val vocab = VocabularyBuilder.build(TokenizerWrapper(JavaTokenizer(), true), psiDir)
        VocabularyBuilder.write(vocab, File(vocabPath.toString()))
    }

    companion object {
        private const val PLUGIN_NAME = "code-completion-benchmark"
        private val vocabPath = Paths.get(PathManager.getPluginsPath(), PLUGIN_NAME, "vocab.txt")
    }
}