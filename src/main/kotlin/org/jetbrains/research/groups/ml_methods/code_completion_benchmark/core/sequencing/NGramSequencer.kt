package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.sequencing

import java.util.*
import kotlin.math.max
import kotlin.math.min

object NGramSequencer {

    fun sequenceForward(tokens: List<Int>, maxOrder: Int): List<List<Int>> {
        val result = ArrayList<List<Int>>()
        for (start in tokens.indices) {
            val end = min(tokens.size, start + maxOrder)
            result.add(tokens.subList(start, end))
        }
        return result
    }

    fun sequenceAround(tokens: List<Int>, index: Int, maxOrder: Int): List<List<Int>> {
        val result = ArrayList<List<Int>>()
        val firstLoc = index - maxOrder + 1
        for (start in max(0, firstLoc)..index) {
            val end = min(tokens.size, start + maxOrder)
            result.add(tokens.subList(start, end))
        }
        return result
    }

    fun sequenceAt(tokens: List<Int>, index: Int, maxOrder: Int): List<Int> {
        return tokens.subList(max(0, index - maxOrder + 1), index + 1)
    }
}