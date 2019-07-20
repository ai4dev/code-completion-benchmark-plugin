package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.counting

import java.io.Externalizable
import java.util.function.Consumer

interface Counter : Externalizable {

    val count: Int

    val successorCount: Int

    fun getCounts(indices: List<Int>): LongArray

    /**
     * Returns the number of sequences of length n seen `count' times
     */
    fun getCountOfCount(n: Int, count: Int): Int

    fun getSuccessorCount(indices: List<Int>): Int
    fun getTopSuccessors(indices: List<Int>, limit: Int): List<Int>

    fun getDistinctCounts(range: Int, indices: List<Int>): IntArray

    fun count(indices: List<Int>)
    fun unCount(indices: List<Int>)

    fun countBatch(indices: List<List<Int>>) {
        indices.forEach(Consumer<List<Int>> { idx -> this.count(idx) })
    }

    fun unCountBatch(indices: List<List<Int>>) {
        indices.forEach(Consumer<List<Int>> { idx -> this.unCount(idx) })
    }
}