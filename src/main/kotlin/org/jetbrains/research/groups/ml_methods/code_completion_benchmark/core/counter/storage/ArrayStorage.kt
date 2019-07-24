package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.counter.storage

object ArrayStorage {

    fun checkExactSequence(indices: List<Int>, index: Int, successor: IntArray): Boolean {
        var valid = successor.size == indices.size - index
        if (valid) {
            for (i in 1 until successor.size) {
                if (indices[index + i] != successor[i]) {
                    valid = false
                    break
                }
            }
        }
        return valid
    }

    fun checkPartialSequence(indices: List<Int>, index: Int, successor: IntArray): Boolean {
        var valid = successor.size >= indices.size - index
        if (valid) {
            for (i in 1 until indices.size - index) {
                if (indices[index + i] != successor[i]) {
                    valid = false
                    break
                }
            }
        }
        return valid
    }
}