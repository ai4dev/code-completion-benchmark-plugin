package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.mix

import com.intellij.psi.PsiFile
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.Model
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.PredictionWithConf
import java.io.File
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream

abstract class MixedModel(var left: Model, var right: Model) : Model {

    override var dynamic: Boolean = false
        set(value) {
            left.dynamic = value
            right.dynamic = value
            field = value
        }

    override fun notify(next: PsiFile) {
        notifyLeft(next)
        notifyRight(next)
    }

    protected fun notifyLeft(next: PsiFile) {
        left.notify(next)
    }

    protected open fun notifyRight(next: PsiFile) {
        right.notify(next)
    }


    override fun pauseDynamic() {
        left.pauseDynamic()
        right.pauseDynamic()
    }

    override fun unPauseDynamic() {
        left.unPauseDynamic()
        right.unPauseDynamic()
    }

    override fun learn(input: List<Int>) {
        learnLeft(input)
        learnRight(input)
    }

    protected fun learnLeft(input: List<Int>) {
        left.learn(input)
    }

    protected open fun learnRight(input: List<Int>) {
        right.learn(input)
    }

    override fun learnToken(input: List<Int>, index: Int) {
        learnLeft(input, index)
        learnRight(input, index)
    }

    protected fun learnLeft(input: List<Int>, index: Int) {
        left.learnToken(input, index)
    }

    protected open fun learnRight(input: List<Int>, index: Int) {
        right.learnToken(input, index)
    }

    override fun forget(input: List<Int>) {
        forgetLeft(input)
        forgetRight(input)
    }

    protected fun forgetLeft(input: List<Int>) {
        left.forget(input)
    }

    protected open fun forgetRight(input: List<Int>) {
        right.forget(input)
    }

    override fun forgetToken(input: List<Int>, index: Int) {
        forgetLeft(input, index)
        forgetRight(input, index)
    }

    protected fun forgetLeft(input: List<Int>, index: Int) {
        left.forgetToken(input, index)
    }

    protected open fun forgetRight(input: List<Int>, index: Int) {
        right.forgetToken(input, index)
    }

    override fun model(input: List<Int>): List<PredictionWithConf> {
        val modelL = modelLeft(input)
        val modelR = modelRight(input)
        return IntStream.range(0, input.size)
                .mapToObj { i -> mix(input, i, modelL[i], modelR[i]) }
                .collect(Collectors.toList())
    }

    protected fun modelLeft(input: List<Int>): List<PredictionWithConf> {
        return left.model(input)
    }

    protected open fun modelRight(input: List<Int>): List<PredictionWithConf> {
        return right.model(input)
    }

    override fun modelToken(input: List<Int>, index: Int): PredictionWithConf {
        val res1 = modelLeft(input, index)
        val res2 = modelRight(input, index)
        return mix(input, index, res1, res2)
    }

    protected fun modelLeft(input: List<Int>, index: Int): PredictionWithConf {
        return left.modelToken(input, index)
    }

    protected open fun modelRight(input: List<Int>, index: Int): PredictionWithConf {
        return right.modelToken(input, index)
    }

    override fun predict(input: List<Int>): List<Map<Int, PredictionWithConf>> {
        val predictL = predictLeft(input)
        val predictR = predictRight(input)
        return IntStream.range(0, input.size)
                .mapToObj { i -> mix(input.toMutableList(), i, predictL[i], predictR[i]) }
                .collect(Collectors.toList())
    }


    protected fun predictLeft(input: List<Int>): List<Map<Int, PredictionWithConf>> {
        return left.predict(input)
    }

    protected open fun predictRight(input: List<Int>): List<Map<Int, PredictionWithConf>> {
        return right.predict(input)
    }

    override fun predictToken(input: List<Int>, index: Int): Map<Int, PredictionWithConf> {
        val res1 = predictLeft(input, index)
        val res2 = predictRight(input, index)
        return mix(input.toMutableList(), index, res1, res2)
    }

    protected fun predictLeft(input: List<Int>, index: Int): Map<Int, PredictionWithConf> {
        return left.predictToken(input, index)
    }

    protected open fun predictRight(input: List<Int>, index: Int): Map<Int, PredictionWithConf> {
        return right.predictToken(input, index)
    }

    protected abstract fun mix(
            input: List<Int>,
            index: Int,
            res1: PredictionWithConf,
            res2: PredictionWithConf
    ): PredictionWithConf

    protected fun mix(
            input: MutableList<Int>, index: Int,
            res1: Map<Int, PredictionWithConf>, res2: Map<Int, PredictionWithConf>
    ): Map<Int, PredictionWithConf> {
        val mixed = HashMap<Int, PredictionWithConf>()
        left.pauseDynamic()
        right.pauseDynamic()

        for (key in res1.keys) {
            val own = res1[key]
            var other: PredictionWithConf? = res2[key]
            if (other == null) {
                val added = index == input.size
                if (added) input.add(0)

                val prev = input.set(index, key)
                other = modelRight(input, index)
                if (added)
                    input.removeAt(input.size - 1)
                else
                    input[index] = prev
            }
            mixed[key] = mix(input, index, own!!, other)
        }
        for (key in res2.keys) {
            if (res1.containsKey(key)) continue
            val own = res2[key]
            val added = index == input.size
            if (added) input.add(0)

            val prev = input.set(index, key)
            val other = modelLeft(input, index)
            if (added)
                input.removeAt(input.size - 1)
            else
                input[index] = prev
            mixed[key] = mix(input, index, own!!, other)
        }
        left.unPauseDynamic()
        right.unPauseDynamic()
        return mixed
    }

    override fun toString(): String {
        return this.javaClass.simpleName + "[" + left.toString() + ", " + right.toString() + "]"
    }

    override fun save(directory: File) {
        getLeftDirectoryName(directory).apply {
            mkdir()
            left.save(this)
        }

        getRightDirectoryName(directory).apply {
            mkdir()
            right.save(this)
        }
    }

    override fun load(directory: File): MixedModel {
        val leftModel = left.load(getLeftDirectoryName(directory))
        val rightModel = right.load(getRightDirectoryName(directory))

        return defaultModel(leftModel, rightModel)
    }

    protected fun getLeftDirectoryName(directory: File) =
            File("${directory.absolutePath}${File.separator}left")

    protected fun getRightDirectoryName(directory: File) =
            File("${directory.absolutePath}${File.separator}right")

    companion object {

        fun defaultModel(model1: Model, model2: Model): MixedModel {
            return InverseMixModel(model1, model2)
        }
    }
}