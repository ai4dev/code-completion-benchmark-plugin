package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.completion.ngram.components

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.completion.ngram.NGram
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.tokenizers.JavaTokenizer
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.wrappers.TokenizerWrapper
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.mix.InverseMixModel
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.runners.ModelRunner
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary.Vocabulary

import java.nio.file.Paths

class ModelComponent : ProjectComponent {

    override fun projectOpened() {
        ProjectManager.getInstance().openProjects.forEach { proj ->
            val projDir = proj.guessProjectDir() ?: return
            PsiManager.getInstance(proj).findDirectory(projDir)?.let { psiProjDir ->
                model.learnDirectory(psiProjDir)
            }
        }
    }

    companion object {
        const val PLUGIN_NAME = "code-completion-benchmark"

        val vocabPath = Paths.get(PathManager.getPluginsPath(), PLUGIN_NAME, "vocab.txt")
        val model = ModelRunner(InverseMixModel(), TokenizerWrapper(JavaTokenizer(), true), Vocabulary())

        private fun train(path: PsiFile) {
            when (path) {
                is PsiDirectory -> model.learnDirectory(path)
                else            -> model.learnFile(path)
            }
        }

        fun getSuggestion(tokens: List<String>): String {
            return getSuggestionWithProbability(tokens).first
        }

        fun getSuggestionWithProbability(tokens: List<String>): Pair<String, Double> {
            val queryIndices = model.vocabulary.toIndices(tokens).filterNotNull()
            val prediction = model.model.predictToken(queryIndices, tokens.size - 1)
                    .toList()
                    .maxBy { (_, value) -> value.probability }!!

            return Pair(model.vocabulary.toWord(prediction.first), prediction.second.probability)
        }

        fun getTopSuggestions(code: List<String>, maxNumberOfSuggestions: Int = 5): List<String> {
            return getTopSuggestionsWithProbabilities(code, maxNumberOfSuggestions)
                    .map { it.first }
        }

        fun getTopSuggestionsWithProbabilities(tokens: List<String>, maxNumberOfSuggestions: Int = 5): List<Pair<String, Double>> {
            val queryIndices = model.vocabulary.toIndices(tokens).filterNotNull()

            return model.model.predictToken(queryIndices, tokens.size - 1)
                    .toList()
                    .map { (key, value) -> Pair(model.vocabulary.toWord(key), value.probability) }
                    .sortedByDescending { (_, value) -> value }
                    .take(maxNumberOfSuggestions)
        }

        fun getNGramSuggestions(nGram: NGram): List<String> {
            return getTopSuggestions(nGram.elements)
        }
    }

}