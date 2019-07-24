package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary

import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap

fun List<String>.clearSentenceMarkers(): List<String> {
    return this.filter { it !in listOf(Vocabulary.BEGIN_STRING, Vocabulary.END_STRING) }
}

open class Vocabulary : Serializable {

    val wordIndices: MutableMap<String, Int> = HashMap()
    val words: MutableList<String> = ArrayList()
    val counts: MutableList<Int> = ArrayList()

    private var closed: Boolean = false

    private var checkPoint: Int = 0

    init {
        addUnk()
    }

    private fun addUnk() {
        wordIndices[UNKNOWN_TOKEN] = 0
        words.add(UNKNOWN_TOKEN)
        counts.add(0)
    }

    fun size(): Int {
        return words.size
    }

    fun close() {
        closed = true
    }

    fun open() {
        closed = false
    }

    fun setCheckpoint() {
        checkPoint = words.size
    }

    fun restoreCheckpoint() {
        for (i in words.size downTo checkPoint + 1) {
            counts.removeAt(counts.size - 1)
            val word = words.removeAt(words.size - 1)
            wordIndices.remove(word)
        }
    }

    fun store(token: String, count: Int = 1): Int {
        var index: Int? = wordIndices[token]

        index?.let { idx ->
            counts[idx] = count
            return idx
        }

        index = wordIndices.size
        wordIndices[token] = index
        words.add(token)
        counts.add(count)

        return index
    }

    fun toIndices(tokens: Sequence<String>): Sequence<Int> {
        return tokens.map { toIndex(it) }
    }

    fun toIndices(tokens: List<String>): List<Int?> {
        return tokens.map { toIndex(it) }
    }

    fun toIndex(token: String): Int {
        val index: Int? = wordIndices[token]
        index ?: run {
            return if (closed) {
                wordIndices[UNKNOWN_TOKEN]!!
            } else {
                val idx = wordIndices.size
                wordIndices[token] = idx
                words.add(token)
                counts.add(1)
                idx
            }
        }
        return index!!
    }

    fun getCount(token: String): Int? {
        val index = wordIndices[token]
        return index?.let { getCount(it) } ?: 0
    }

    private fun getCount(index: Int?): Int? {
        return counts[index!!]
    }

    fun toWords(indices: Sequence<Int>): Sequence<String> {
        return indices.map { toWord(it) }
    }

    fun toWords(indices: List<Int>): List<String> {
        return indices.map { toWord(it) }
    }

    fun toWord(index: Int): String {
        return words[index]
    }

    companion object {
        const val UNKNOWN_TOKEN = "<unk>"
        const val BEGIN_STRING = "<s>"
        const val END_STRING = "</s>"
    }
}