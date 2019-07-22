package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.counting.storage

import java.io.ObjectInput
import java.io.ObjectOutput
import java.util.ArrayList
import java.util.HashMap
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Comparator
import kotlin.Int
import kotlin.IntArray
import kotlin.math.min

class MapTrieCounter(initSize: Int = 1) : BaseTrieCounter() {

    private var map: HashMap<Int, Any> = HashMap(initSize)
    private val pseudoOrdering: MutableList<Int> = arrayListOf()

    override val successors: Array<Any?>
        get() = map.keys.stream().toArray()

    public override fun getTopSuccessorsInternal(limit: Int): List<Int> {
        val classKey = hashCode()
        val countsKey = keyCode()
        val cached = cache[classKey]
        if (cached == null || cached != countsKey) {
            pseudoOrdering.sortWith(Comparator { i1, i2 -> compareCounts(i1, i2) })
        }
        val end = min(pseudoOrdering.size, limit)
        val topSuccessors = ArrayList(pseudoOrdering.subList(0, end))
        if (successorCount > 10) cache[classKey] = countsKey
        return topSuccessors
    }

    private fun keyCode(): Int {
        return 31 * (successorCount + 31 * count)
    }

    override fun makeNext(depth: Int): BaseTrieCounter {
        return if (depth <= MAX_DEPTH_MAP_TRIE) MapTrieCounter(1) else ArrayTrieCounter()
    }

    override fun getSuccessor(key: Int): Any? {
        return map[key]
    }

    override fun putSuccessor(key: Int, o: Any) {
        val curr = map.put(key, o)
        if (curr == null) pseudoOrdering.add(key)
    }

    private fun compareCounts(i1: Int?, i2: Int?): Int {
        val base = -getCount(map[i1 as Int]).compareTo(getCount(map[i2 as Int]))
        return if (base != 0) base else i1.compareTo(i2)
    }

    override fun removeSuccessor(key: Int) {
        val removed = map.remove(key)
        pseudoOrdering.remove(key)
        if (removed is MapTrieCounter) {
            cache.remove(removed.hashCode())
        }
    }

    override fun readExternal(`in`: ObjectInput) {
        counts = IntArray(2 + COUNT_OF_COUNTS_CUTOFF)
        counts[0] = `in`.readInt()
        counts[1] = `in`.readInt()
        val successors = `in`.readInt()
        map = HashMap(successors, 0.9f)
        var pos = 0
        while (pos < successors) {
            val key = `in`.readInt()
            val code = `in`.readInt()
            val value: Any
            if (code < 0) {
                if (code < -1)
                    value = ArrayTrieCounter()
                else
                    value = MapTrieCounter()
                (value as BaseTrieCounter).readExternal(`in`)
                counts[1 + min(value.count, COUNT_OF_COUNTS_CUTOFF)]++
            } else {
                value = IntArray(code)
                for (j in 0 until code) value[j] = `in`.readInt()
                counts[1 + min(value[0], COUNT_OF_COUNTS_CUTOFF)]++
            }

            putSuccessor(key, value)
            pos++
        }
    }

    override fun writeExternal(out: ObjectOutput) {
        out.writeInt(counts[0])
        out.writeInt(counts[1])
        out.writeInt(map.size)
        for ((key, value) in map.entries) {
            out.writeInt(key)
            if (value is IntArray) {
                out.writeInt(value.size)
                for (j in value.indices) out.writeInt(value[j])
            } else {
                when (value) {
                    is ArrayTrieCounter -> out.writeInt(-2)
                    else                -> out.writeInt(-1)
                }
                (value as? BaseTrieCounter)?.writeExternal(out) ?: return
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapTrieCounter

        if (map != other.map) return false
        if (pseudoOrdering != other.pseudoOrdering) return false

        return true
    }

    override fun hashCode(): Int {
        var result = map.hashCode()
        result = 31 * result + pseudoOrdering.hashCode()
        return result
    }

    companion object {
        private const val MAX_DEPTH_MAP_TRIE = 1

        private val cache = HashMap<Int, Int>()
    }
}