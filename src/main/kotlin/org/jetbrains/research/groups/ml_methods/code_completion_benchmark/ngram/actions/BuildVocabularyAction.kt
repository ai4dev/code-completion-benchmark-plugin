package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.lang.tokenizers.JavaTokenizer
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.lang.wrappers.TokenizerWrapper
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.services.components.NGramModelComponent
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.vocabulary.builders.VocabularyBuilder

class BuildVocabularyAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val proj = event.project ?: return
        val projDir = proj.guessProjectDir() ?: return
        val psiDir = PsiManager.getInstance(proj).findDirectory(projDir) ?: return
        VocabularyBuilder.build(TokenizerWrapper(JavaTokenizer(), true), psiDir).let { vocab ->
            VocabularyBuilder.write(vocab, NGramModelComponent.vocabPath.toFile())
        }

    }
}