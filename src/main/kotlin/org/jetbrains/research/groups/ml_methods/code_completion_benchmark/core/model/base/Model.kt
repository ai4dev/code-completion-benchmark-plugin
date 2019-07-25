package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base

import com.intellij.psi.PsiFile
import java.io.File

interface Model {

    var dynamic: Boolean

    fun notify(next: PsiFile)

    fun pauseDynamic()
    fun unPauseDynamic()

    fun learn(input: List<Int>)

    fun learnToken(input: List<Int>, index: Int)

    fun forget(input: List<Int>)

    fun forgetToken(input: List<Int>, index: Int)

    fun getConfidence(input: List<Int>, index: Int): Double = 0.0

    fun model(input: List<Int>): List<ConfPrediction>

    fun modelToken(input: List<Int>, index: Int): ConfPrediction

    fun predict(input: List<Int>): List<Map<Int, ConfPrediction>>

    fun predictToken(input: List<Int>, index: Int): Map<Int, ConfPrediction>

    fun save(directory: File)

    fun load(directory: File): Model

}