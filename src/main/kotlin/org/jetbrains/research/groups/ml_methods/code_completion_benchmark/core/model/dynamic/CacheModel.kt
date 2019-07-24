package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.dynamic

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.BaseModel
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.Model
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.base.ConfPrediction
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.ngrams.NGramModel

import java.io.File
import java.util.*

class CacheModel(
        private var model: Model = NGramModel.defaultModel(),
        private val capacity: Int = DEFAULT_CAPACITY
) : BaseModel() {

    private val cache: Deque<Pair<List<Int>, Int>> = ArrayDeque(capacity)
    private val cachedRefs: MutableMap<Int, List<Int>> = HashMap()

    init {
        dynamic = true
    }

    override fun notify(next: File) {
        model = try {
            model.javaClass.getConstructor().newInstance()
        } catch (e: Exception) {
            NGramModel.defaultModel()
        }

        cache.clear()
        cachedRefs.clear()
    }

    override fun learn(input: List<Int>) {}

    override fun learnToken(input: List<Int>, index: Int) {}
    override fun forget(input: List<Int>) {}
    override fun forgetToken(input: List<Int>, index: Int) {}

    override fun modelAtIndex(input: List<Int>, index: Int): ConfPrediction {
        val modeled = model.modelToken(input, index)
        updateCache(input, index)
        return modeled
    }

    private fun updateCache(input: List<Int>, index: Int) {
        if (capacity > 0 && dynamic) {
            store(input, index)
            model.learnToken(input, index)
            if (cache.size > capacity) {
                val removed = cache.removeFirst()
                model.forgetToken(removed.first, removed.second)
            }
        }
    }

    private fun store(input: List<Int>, index: Int) {
        val hash = input.hashCode()
        var list: List<Int>? = cachedRefs[hash]
        if (list == null) {
            list = ArrayList(input)
            cachedRefs[hash] = list
        }
        cache.addLast(Pair(list, index))
    }

    override fun predictAtIndex(input: List<Int>?, index: Int): Map<Int, ConfPrediction> {
        return model.predictToken(input!!, index)
    }


    override fun toString(): String {
        return javaClass.simpleName
    }

    //TODO save cache or not?
    override fun save(directory: File) {
        model.save(directory)
    }

    override fun load(directory: File): Model {
        return CacheModel(model.load(directory), capacity)
    }


    companion object {
        const val DEFAULT_CAPACITY = 5000
    }
}