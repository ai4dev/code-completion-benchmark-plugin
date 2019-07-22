package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.modeling.base

import java.io.File

interface Model {

    fun notify(next: File)

    var dynamic: Boolean

    fun pauseDynamic()
    fun unPauseDynamic()

    fun learn(input: List<Int>)

    fun learnToken(input: List<Int>, index: Int)

    fun forget(input: List<Int>)

    fun forgetToken(input: List<Int>, index: Int)

    fun getConfidence(input: List<Int>, index: Int): Double = 0.0

    fun model(input: List<Int>): List<Prediction>

    fun modelToken(input: List<Int>, index: Int): Prediction

    fun predict(input: List<Int>): List<Map<Int, Prediction>>

    fun predictToken(input: List<Int>, index: Int): Map<Int, Prediction>

    fun save(directory: File)

    fun load(directory: File): Model

}