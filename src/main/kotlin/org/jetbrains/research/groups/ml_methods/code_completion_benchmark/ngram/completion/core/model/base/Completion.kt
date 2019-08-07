package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.completion.core.model.base

data class Completion(val realIx: Int?, val predictions: List<Pair<Int, Double>>)