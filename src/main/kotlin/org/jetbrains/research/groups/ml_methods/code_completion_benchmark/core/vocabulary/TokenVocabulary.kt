package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.vocabulary

import java.io.Serializable

open class TokenVocabulary : Serializable {

    val wordIndices: MutableMap<String, Int> = HashMap()
    val words: MutableList<String> = ArrayList()
    val counts: MutableList<Int> = ArrayList()

    var closed: Boolean = false

    var checkPoint: Int = 0

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

    fun store(token: String, count: Int = 1): Int {
        var index: Int? = wordIndices[token]
        if (index == null) {
            index = wordIndices.size
            wordIndices[token] = index
            words.add(token)
            counts.add(count)
        } else {
            counts[index] = count
        }
        return index
    }

    fun toIndices(tokens: Sequence<String>): Sequence<Int> {
        return tokens.map { toIndex(it) }
    }

    fun toIndices(tokens: List<String>): List<Int> {
        return tokens.map { toIndex(it) }
    }

    fun toIndex(token: String): Int {
        var index: Int? = wordIndices[token]
        if (index == null) {
            if (closed) {
                return wordIndices[UNKNOWN_TOKEN]!!
            } else {
                index = wordIndices.size
                wordIndices[token] = index
                words.add(token)
                counts.add(1)
            }
        }
        return index
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