package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.completion.core.model.ngrams

import com.intellij.psi.PsiFile
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.completion.core.model.base.BaseModel
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.completion.core.model.base.PredictionWithConf
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.counter.Counter
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.counter.storage.MapTrieCounter
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.sequencer.NGramSequencer
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.services.NGram
import java.util.stream.Collectors
import kotlin.math.pow

abstract class NGramModel(
        val order: Int = NGram.DEFAULT_NGRAM_ORDER,
        var counter: Counter = MapTrieCounter()
) : BaseModel() {

    override fun notify(next: PsiFile) {}

    override fun learn(input: List<Int>) {
        this.counter.countBatch(NGramSequencer.sequenceForward(input, this.order))
    }

    override fun learnToken(input: List<Int>, index: Int) {
        val sequence = NGramSequencer.sequenceAt(input, index, this.order)
        for (i in sequence.indices) {
            this.counter.count(sequence.subList(i, sequence.size))
        }
    }

    override fun forget(input: List<Int>) {
        this.counter.unCountBatch(NGramSequencer.sequenceForward(input, this.order))
    }

    override fun forgetToken(input: List<Int>, index: Int) {
        val sequence = NGramSequencer.sequenceAt(input, index, this.order)
        for (i in sequence.indices) {
            this.counter.unCount(sequence.subList(i, sequence.size))
        }
    }

    override fun modelAtIndex(input: List<Int>, index: Int): PredictionWithConf {
        val sequence = NGramSequencer.sequenceAt(input, index, this.order)
        var probability = 0.0
        var mass = 0.0
        var hits = 0
        for (i in sequence.indices.reversed()) {
            val sub = sequence.subList(i, sequence.size)
            val counts = this.counter.getCounts(sub)
            if (counts[1] == 0L) break
            val resN = this.modelWithConfidence(sub, counts)
            val prob = resN.probability
            val conf = resN.confidence
            mass = (1 - conf) * mass + conf
            probability = (1 - conf) * probability + conf * prob
            hits++
        }
        if (mass > 0) probability /= mass
        // In the new model, final confidence is asymptotically close to 1 for all n-gram models
        val confidence = 1 - 2.0.pow((-hits).toDouble())
        return PredictionWithConf(probability, confidence)
    }

    protected abstract fun modelWithConfidence(subList: List<Int>, counts: LongArray): PredictionWithConf

    override fun predictAtIndex(input: List<Int>, index: Int): Map<Int, PredictionWithConf> {
        val inputRev = input.reversed()
        val sequence = NGramSequencer.sequenceAt(inputRev, index, order)
        val predictions = HashSet<Int>()

        for (i in 0..sequence.size) {
            val limit = GLOBAL_PREDICTION_CUTOFF - predictions.size
            if (limit <= 0) break
            predictions.addAll(counter.getTopSuccessors(sequence.subList(i, sequence.size), limit))
        }
        return predictions.stream().collect(
            Collectors.toMap({ p -> p },
                { p -> prob(input.toMutableList(), index, p!!) })
        )
    }

    private fun prob(input: MutableList<Int>, index: Int, prediction: Int): PredictionWithConf {
        val added = index == input.size
        if (added) input.add(0)
        val prev = input.set(index, prediction)
        val prob = this.modelAtIndex(input, index)
        if (added)
            input.removeAt(input.size - 1)
        else
            input[index] = prev
        return prob
    }

    override fun toString(): String {
        return this.javaClass.simpleName
    }

    companion object {
        const val GLOBAL_PREDICTION_CUTOFF = 10

        private var standard: Class<out NGramModel> = JMModel::class.java
        fun setStandard(clazz: Class<out NGramModel>) {
            standard = clazz
        }

        fun defaultModel(): NGramModel {
            return try {
                standard.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                e.printStackTrace()
                JMModel()
            }
        }

        fun defaultModel(counter: Counter): NGramModel {
            try {
                return standard.getConstructor(Counter::class.java).newInstance(counter)
            } catch (e: Exception) {
                e.printStackTrace()
                return JMModel()
            }
        }
    }
}