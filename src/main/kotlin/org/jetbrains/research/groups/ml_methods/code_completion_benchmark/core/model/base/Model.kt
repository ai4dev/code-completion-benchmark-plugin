package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base

import com.intellij.psi.PsiFile
import java.io.File

interface Model {

    fun notify(next: PsiFile)

    var dynamic: Boolean

    fun pauseDynamic()
    fun unPauseDynamic()

    fun learn(input: List<Int>) {
        (0 until input.size).forEach { learnToken(input, it) }
    }

    fun learnToken(input: List<Int>, index: Int)

    fun forget(input: List<Int>) {
        (0 until input.size).forEach { forgetToken(input, it) }
    }

    fun forgetToken(input: List<Int>, index: Int)

    fun getConfidence(input: List<Int>, index: Int): Double {
        return 0.0
    }

    fun model(input: List<Int>): List<ConfPrediction> {
        return (0 until input.size)
                .map { modelToken(input, it) }
    }

    fun modelToken(input: List<Int>, index: Int): ConfPrediction

    fun predict(input: List<Int>): List<Map<Int, ConfPrediction>> {
        return (0 until input.size)
                .map { predictToken(input, it) }
    }

    fun predictToken(input: List<Int>, index: Int): Map<Int, ConfPrediction>

    fun save(directory: File)

    fun load(directory: File): Model
}