package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.counter.storage

import java.io.ObjectInput
import java.io.ObjectOutput
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.min

class ArrayTrieCounter(initSize: Int = 1) : BaseTrieCounter() {

    var indices: IntArray = IntArray(initSize)

    override var successors: Array<Any?> = arrayOfNulls(initSize)

    fun getSuccessors() = indices
            .filter { it != Integer.MAX_VALUE }
            .map { Integer.valueOf(it) }
            .toTypedArray()

    init {
        Arrays.fill(indices, Integer.MAX_VALUE)
    }

    public override fun getTopSuccessorsInternal(limit: Int): List<Int> {
        return IntStream.range(0, indices.size)
                .filter { i -> indices[i] != Integer.MAX_VALUE }
                .mapToObj { i -> Pair(indices[i], getCount(successors[i])) }
                .filter { p -> p.second > 0 }
                .sorted { p1, p2 -> -p1.second.compareTo(p2.second) }
                .limit(limit.toLong())
                .map { p -> p.first }
                .collect(Collectors.toList())
    }

    override fun makeNext(depth: Int): BaseTrieCounter {
        return ArrayTrieCounter()
    }

    override fun getSuccessor(key: Int): Any? {
        val ix = getSuccIx(key)
        return if (ix < 0) null else successors[ix]
    }

    override fun removeSuccessor(key: Int) {
        val ix = getSuccIx(key)
        if (ix >= 0) {
            if (ix < indices.size - 1) {
                val indLen = indices.size - ix - 1
                val succLen = successors.size - ix - 1
                System.arraycopy(indices, ix + 1, indices, ix, indLen)
                System.arraycopy(successors, ix + 1, successors, ix, succLen)
            }

            indices[indices.size - 1] = Integer.MAX_VALUE
            val padding = getSuccIx(Integer.MAX_VALUE)
            if (padding >= 5 && padding < indices.size / 2) {
                indices = indices.copyOf(padding + 1)
                successors = successors.copyOf(padding + 1)
            }
        }
    }

    override fun putSuccessor(key: Int, o: Any) {
        var ix = getSuccIx(key)
        if (ix >= 0) {
            successors[ix] = o
        } else {
            ix = -ix - 1
            if (ix >= indices.size) grow()
            if (indices[ix] != Integer.MAX_VALUE) {
                System.arraycopy(indices, ix, indices, ix + 1, indices.size - ix - 1)
                System.arraycopy(successors, ix, successors, ix + 1, successors.size - ix - 1)
            }
            indices[ix] = key
            successors[ix] = o
            if (indices[indices.size - 1] != Integer.MAX_VALUE) grow()
        }
    }

    private fun getSuccIx(key: Int): Int {
        return if (indices.size > 1000 && key > 0 && key <= indices.size && indices[key - 1] == key)
            key - 1
        else
            Arrays.binarySearch(indices, key)
    }

    private fun grow() {
        val oldLen = indices.size
        var newLen = (indices.size * GROWTH_FACTOR + 1).toInt()
        if (newLen == oldLen - 1) newLen++
        indices = indices.copyOf(newLen)
        successors = successors.copyOf(newLen)
        for (i in oldLen until indices.size) indices[i] = Integer.MAX_VALUE
    }

    override fun readExternal(`in`: ObjectInput) {
        counts = IntArray(2 + COUNT_OF_COUNTS_CUTOFF)
        counts[0] = `in`.readInt()
        counts[1] = `in`.readInt()
        val newSuccessors = `in`.readInt()
        indices = IntArray(newSuccessors)
        successors = arrayOfNulls(newSuccessors)
        var pos = 0
        while (pos < newSuccessors) {
            val key = `in`.readInt()
            val code = `in`.readInt()
            val value: Any
            if (code < 0) {
                value = `in`.readObject()
                counts[1 + min((value as BaseTrieCounter).count, COUNT_OF_COUNTS_CUTOFF)]++
            } else {
                value = IntArray(code)
                for (j in 0 until code) value[j] = `in`.readInt()
                counts[1 + min(value[0], COUNT_OF_COUNTS_CUTOFF)]++
            }
            indices[pos] = key
            successors[pos] = value
            pos++
        }
    }

    override fun writeExternal(out: ObjectOutput) {
        out.writeInt(counts[0])
        out.writeInt(counts[1])
        out.writeInt(successorCount)
        for (i in indices.indices) {
            if (indices[i] == Integer.MAX_VALUE) continue
            out.writeInt(indices[i])
            when (val o = successors[i]) {
                is IntArray -> {
                    out.writeInt(o.size)
                    for (j in o.indices) out.writeInt(o[j])
                }
                else        -> {
                    out.writeInt(-1)
                    out.writeObject(o as BaseTrieCounter)
                }
            }
        }
    }

    companion object {
        private const val GROWTH_FACTOR = 1.5
    }
}