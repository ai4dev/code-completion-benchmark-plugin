package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.counter.storage

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.counter.Counter
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.model.runners.ModelRunner
import java.io.ObjectInput
import java.io.ObjectOutput
import java.util.*
import kotlin.math.min

abstract class BaseTrieCounter : Counter {

    var counts = IntArray(2 + COUNT_OF_COUNTS_CUTOFF)

    abstract val successors: Array<Any?>

    val contextCount = counts[1]

    internal abstract fun makeNext(depth: Int): BaseTrieCounter

    abstract fun getSuccessor(key: Int): Any?

    internal abstract fun getTopSuccessorsInternal(limit: Int): List<Int>

    internal abstract fun putSuccessor(key: Int, o: Any)
    internal abstract fun removeSuccessor(key: Int)

    abstract override fun readExternal(`in`: ObjectInput)

    abstract override fun writeExternal(out: ObjectOutput)


    override val count = counts[0]

    internal fun getCount(successor: Any?): Int {
        successor ?: return 0
        return (successor as? BaseTrieCounter)?.count ?: (successor as IntArray?)!![0]
    }

    override fun getCountOfCount(n: Int, count: Int): Int {
        return nCounts[n - 1][count - 1]
    }

    override fun getCounts(indices: List<Int>): LongArray {
        return if (indices.isEmpty()) longArrayOf(count.toLong(), count.toLong()) else getCounts(indices, 0)
    }

    private fun getCounts(indices: List<Int>, index: Int): LongArray {
        val next = indices[index]
        val succ = getSuccessor(next)
        val nearLast = index == indices.size - 1

        if (succ != null && succ is BaseTrieCounter) {
            return if (!nearLast)
                succ.getCounts(indices, index + 1)
            else
                longArrayOf(succ.count.toLong(), counts[1].toLong())
        }

        val localCounts = LongArray(2)
        if (nearLast)
            localCounts[1] = counts[1].toLong()
        if (succ != null) {
            val successor = succ as IntArray
            if (ArrayStorage.checkPartialSequence(indices, index, successor)) {
                localCounts[0] = successor[0].toLong()
                if (!nearLast)
                    localCounts[1] = localCounts[0]
            } else if (!nearLast
                    && successor.size >= indices.size - index
                    && ArrayStorage.checkPartialSequence(indices.subList(0, indices.size - 1), index, successor)
            ) {
                localCounts[1] = successor[0].toLong()
            }
        }
        return localCounts
    }

    override fun getDistinctCounts(range: Int, indices: List<Int>): IntArray {
        return getDistinctCounts(range, indices, 0)
    }

    private fun getDistinctCounts(range: Int, indices: List<Int>, index: Int): IntArray {
        if (index < indices.size) {
            val next = indices[index]
            val succ = getSuccessor(next) ?: return IntArray(range)

            return if (succ is BaseTrieCounter) {
                succ.getDistinctCounts(range, indices, index + 1)
            } else {
                val successor = succ as IntArray
                val distinctCounts = IntArray(range)
                if (ArrayStorage.checkPartialSequence(indices, index, successor)
                        && !ArrayStorage.checkExactSequence(indices, index, successor)
                ) {
                    distinctCounts[min(range - 1, successor[0] - 1)] = 1
                }
                distinctCounts
            }
        } else {
            val distinctCounts = IntArray(range)
            var totalDistinct = successorCount
            var i = 2
            while (i < counts.size - 1 && i - 1 < range) {
                val countOfCountsI = counts[i]
                distinctCounts[i - 2] = countOfCountsI
                totalDistinct -= countOfCountsI
                i++
            }
            distinctCounts[range - 1] = totalDistinct
            return distinctCounts
        }
    }

    override val successorCount: Int =
            counts.drop(2).sum()

    override fun getSuccessorCount(indices: List<Int>): Int {

        return when (val successor = getSuccessorNode(indices, 0)) {
            null               -> 0
            is BaseTrieCounter -> successor.successorCount
            else               -> 1
        }
    }

    private fun getSuccessorNode(indices: List<Int>, index: Int): Any? {
        if (index == indices.size)
            return this

        val next = indices[index]

        return when (val succ = getSuccessor(next)) {
            null               -> null
            is BaseTrieCounter -> succ.getSuccessorNode(indices, index + 1)
            else               -> {
                val successor = succ as IntArray

                if (!ArrayStorage.checkPartialSequence(indices, index, successor))
                    return null

                val trueSucc = IntArray(1 + successor.size - (indices.size - index))
                trueSucc[0] = successor[0]

                for (i in 1 until trueSucc.size)
                    trueSucc[i] = successor[i + indices.size - index - 1]

                return trueSucc
            }
        }
    }

    override fun getTopSuccessors(indices: List<Int>, limit: Int): List<Int> {
        return when (val successor = getSuccessorNode(indices, 0)) {
            null               -> ArrayList()
            is BaseTrieCounter -> successor.getTopSuccessorsInternal(limit)
            else               -> {
                val succ = successor as IntArray
                val successors = arrayListOf<Int>()
                if (succ.size > 1)
                    successors.add(succ[1])
                successors
            }
        }
    }

    override fun count(indices: List<Int>) {
        update(indices, 1)
    }

    override fun unCount(indices: List<Int>) {
        update(indices, -1)
    }

    fun updateCount(adj: Int) {
        update(emptyList(), adj)
    }

    fun update(indices: List<Int>, adj: Int) {
        update(indices, 0, adj)
    }

    @Synchronized
    private fun update(indices: List<Int>, index: Int, adj: Int) {
        if (index < indices.size) {
            val key = indices[index]
            val successor = getSuccessor(key)
            if (successor != null)
                updateSuccessor(indices, index, adj, successor)
            else
                addArray(indices, index, adj)
        }
        counts[0] += adj
        if (index != indices.size)
            counts[1] += adj
        updateNCounts(index, count, adj)
    }

    private fun updateSuccessor(indices: List<Int>, index: Int, adj: Int, succ: Any) {
        if (succ is BaseTrieCounter)
            updateTrie(indices, index, adj, succ)
        else
            updateArray(indices, index, adj, succ)
    }

    private fun updateTrie(indices: List<Int>, index: Int, adj: Int, succ: Any) {
        var next = succ as BaseTrieCounter
        if (next is ArrayTrieCounter) {
            val arrayCounter = next
            if (arrayCounter.indices.size > 10) {
                next = promoteArrayToMap(indices, index, arrayCounter)
            }
        }
        next.update(indices, index + 1, adj)
        updateCountOfCounts(next.count, adj)
        if (next.count == 0) {
            removeSuccessor(indices[index])
        }
    }

    private fun updateArray(indices: List<Int>, index: Int, adj: Int, succ: Any) {
        val successor = when (succ) {
            is IntArray -> succ
            is Int      -> intArrayOf(succ)
            else        -> throw IllegalArgumentException(succ::class.toString())
        }
        val valid = ArrayStorage.checkExactSequence(indices, index, successor)
        if (valid)
            updateArrayCount(indices, index, adj, successor)
        else {
            val newNext = promoteArrayToTrie(indices, index, successor)
            updateTrie(indices, index, adj, newNext)
        }
    }

    private fun updateArrayCount(indices: List<Int>, index: Int, adj: Int, successor: IntArray) {
        successor[0] += adj
        if (successor[0] == 0) {
            removeSuccessor(indices[index])
        }
        updateCountOfCounts(successor[0], adj)
        for (i in index + 1..indices.size) {
            updateNCounts(i, successor[0], adj)
        }
    }

    private fun promoteArrayToMap(indices: List<Int>, index: Int, counter: ArrayTrieCounter): BaseTrieCounter {
        val newNext = MapTrieCounter()
        newNext.counts = counter.counts

        for (i in counter.indices.indices) {
            val ix = counter.indices[i]
            if (ix == Integer.MAX_VALUE)
                continue
            val successor = counter.successors
            newNext.putSuccessor(ix, successor)
        }
        putSuccessor(indices[index], newNext)
        return newNext
    }

    private fun promoteArrayToTrie(indices: List<Int>, index: Int, successor: IntArray): BaseTrieCounter {
        val newNext = makeNext(index)
        newNext.updateCount(successor[0])
        if (successor.size > 1) {
            newNext.counts[1] = newNext.counts[0]
            val temp = Arrays.copyOfRange(successor, 1, successor.size)
            temp[0] = successor[0]
            newNext.putSuccessor(successor[1], temp)
            if (COUNT_OF_COUNTS_CUTOFF > 0) {
                newNext.counts[1 + min(temp[0], COUNT_OF_COUNTS_CUTOFF)]++
            }
        }
        putSuccessor(indices[index], newNext)
        return newNext
    }

    private fun addArray(indices: List<Int>, index: Int, adj: Int) {
        if (adj < 0) {
            println("Attempting to forget unknown event: " + indices.subList(index, indices.size))
            return
        }
        val singleton = IntArray(indices.size - index)
        singleton[0] = adj
        for (i in 1 until singleton.size) {
            singleton[i] = indices[index + i]
        }
        putSuccessor(indices[index], singleton)
        updateCountOfCounts(adj, adj)
        for (i in index + 1..indices.size) {
            updateNCounts(i, adj, adj)
        }
    }

    private fun updateCountOfCounts(count: Int, adj: Int) {
        if (COUNT_OF_COUNTS_CUTOFF == 0) return

        val currIndex = min(count, COUNT_OF_COUNTS_CUTOFF)
        val prevIndex = min(count - adj, COUNT_OF_COUNTS_CUTOFF)
        if (currIndex != prevIndex) {
            if (currIndex >= 1) counts[currIndex + 1]++
            if (prevIndex >= 1) counts[prevIndex + 1]--
        }
    }

    companion object {

        var COUNT_OF_COUNTS_CUTOFF = 3

        @Volatile
        var nCounts = Array(ModelRunner.DEFAULT_NGRAM_ORDER) { IntArray(4) }

        private fun updateNCounts(n: Int, count: Int, adj: Int) {
            if (n == 0)
                return

            val toUpdate = nCounts[n - 1]
            val currIndex = min(count, toUpdate.size)
            val prevIndex = min(count - adj, toUpdate.size)

            if (currIndex != prevIndex) {
                val updateCurr = currIndex > 0
                val updatePrev = prevIndex > 0

                if (updateCurr)
                    toUpdate[currIndex - 1]++
                if (updatePrev)
                    toUpdate[prevIndex - 1]--
            }
        }
    }
}