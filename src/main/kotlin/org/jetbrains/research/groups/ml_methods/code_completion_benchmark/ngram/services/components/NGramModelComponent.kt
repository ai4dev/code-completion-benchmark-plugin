package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.services.components

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.ProjectComponent
import java.nio.file.Paths

class NGramModelComponent : ProjectComponent {

    override fun projectOpened() {}

    companion object {
        const val PLUGIN_NAME = "code-completion-benchmark"

        val vocabPath = Paths.get(PathManager.getPluginsPath(), PLUGIN_NAME, "vocab.txt")
    }
}