package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang

import java.io.File

interface Tokenizer {

    fun tokenizeFile(file: File): Sequence<Sequence<String>>

    fun tokenizeText(text: String): Sequence<Sequence<String>> {
        return text
                .split('\n')
                .dropLastWhile { it.isEmpty() }
                .map { this.tokenizeLine(it) }
                .asSequence()
    }

    fun tokenizeLine(line: String): Sequence<String>
}

