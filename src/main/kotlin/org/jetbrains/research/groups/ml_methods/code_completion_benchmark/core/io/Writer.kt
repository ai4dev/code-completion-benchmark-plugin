package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.io

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object Writer {

    fun writeContent(file: File, content: String) {
        BufferedWriter(
            OutputStreamWriter(
                FileOutputStream(file),
                StandardCharsets.UTF_8
            )
        ).use { fw -> fw.append(content) }
    }

    fun writeLines(file: File, lines: List<String>) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { fw ->
            lines.forEach {
                fw.append(it)
                fw.append('\n')
            }
        }
    }

    fun <T> writeAny(file: File, lines: List<List<T>>) {
        writeAny(file, lines.asSequence().map { it.asSequence() })
    }

    fun <T> writeAny(file: File, lines: Sequence<Sequence<T>>) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { fw ->
            lines.forEach { l ->
                try {
                    val line = l.toList()
                    line.forEachIndexed { index, t ->
                        fw.append(t.toString())
                        if (index < line.size - 1) {
                            fw.append('\t')
                        }
                    }
                    fw.append('\n')
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun writeTokenized(file: File, lines: List<List<String>>) {
        writeTokenized(file, lines.asSequence().map { it.asSequence() })
    }

    fun writeTokenized(file: File, lines: Sequence<Sequence<String>>) {
        BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { fw ->
            lines.forEach { l ->
                try {
                    val line = l.toList()
                    line.forEachIndexed { index, s ->
                        fw.append(s
                                .replace("\n".toRegex(), "\\n")
                                .replace("\t".toRegex(), "\\t"))
                        if (index < line.size - 1) {
                            fw.append('\n')
                        }
                    }
                    fw.append('\n')
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}