package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.ngrams

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.counter.Counter
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.counter.storage.MapTrieCounter
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.ConfPrediction
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.runners.ModelRunner
import java.io.File

class JMModel(
        order: Int = ModelRunner.DEFAULT_NGRAM_ORDER,
        private val lambda: Double = DEFAULT_LAMBDA,
        counter: Counter = MapTrieCounter()
) : NGramModel(order, counter) {


    override fun modelWithConfidence(subList: List<Int>, counts: LongArray): ConfPrediction {
        val count = counts[0]
        val contextCount = counts[1]

        val mle = count / contextCount.toDouble()
        return ConfPrediction(mle, lambda)
    }

    class JMMConfig(order: Int, name: String, val lambda: Double): Config(order, name)

    override val config = JMMConfig(order, this::class.java.toString(), lambda)

    override fun load(directory: File): JMModel {
        val counter = loadCounter(directory)
        val config = loadConfig<JMMConfig>(directory)

        return JMModel(config.order, config.lambda, counter)
    }

    companion object {
        private const val DEFAULT_LAMBDA = 0.5

        fun load(directory: File) = JMModel().load(directory)
        fun save(directory: File, model: JMModel) = model.save(directory)

    }
}