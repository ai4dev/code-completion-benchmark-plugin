package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.completion.ngram

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.io.DataInputOutputUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.lang.collectors.JavaASTCollector
import java.io.DataInput
import java.io.DataOutput

data class NGram(val elements: List<String>) {

    fun appendToken(token: String): NGram {
        return NGram(listOf(*elements.toTypedArray(), token))
    }

    fun dropHeadToken(): NGram {
        return NGram(elements.drop(1))
    }

    companion object {

        const val DEFAULT_NGRAM_ORDER = 6
        val INVALID_NGRAM = NGram(emptyList())

        fun processFile(psiFile: PsiFile, content: CharSequence): HashMap<NGram, Int> {
            val result = HashMap<NGram, Int>()
            val elements = JavaASTCollector.getElements(psiFile)
            if (elements.size > DEFAULT_NGRAM_ORDER) {
                for (i in DEFAULT_NGRAM_ORDER until elements.size) {
                    if (NGramElementProvider.shouldIndex(elements[i], content)) {
                        val nGramElements = ArrayList<String>()
                        (1..DEFAULT_NGRAM_ORDER).forEach { j ->
                            nGramElements.add(getElementRepresentation(elements[i - j]))
                        }
                        nGramElements.add(elements[i].text)
                        (0..DEFAULT_NGRAM_ORDER).forEach { j ->
                            val ngram = NGram(nGramElements.subList(j, nGramElements.size))
                            val oldValue = result.putIfAbsent(ngram, 1)
                            oldValue?.let {
                                result[ngram] = oldValue + 1
                            }
                        }
                    }
                }
            }
            return result
        }

        fun getNGramForElement(element: PsiElement): NGram {
            val elements = JavaASTCollector.getElements(element.containingFile ?: return INVALID_NGRAM)
            val index = elements.indexOf(element.node)
            if (index == -1 || index < DEFAULT_NGRAM_ORDER) {
                return INVALID_NGRAM
            }
            val nGramElements = ArrayList<String>()
            (1..DEFAULT_NGRAM_ORDER).forEach { i ->
                nGramElements.add(getElementRepresentation(elements[index - i]))
            }
            println(nGramElements.reversed())
            return NGram(nGramElements)
        }

        fun getElementRepresentation(element: ASTNode): String {
            return element.text
        }
    }
}

object NGramKeyDescriptor: KeyDescriptor<NGram> {
    override fun save(out: DataOutput, nGram: NGram?) {
        val instance = NGramEnumeratingService.getInstance()
        out.writeINT(nGram!!.elements.size)
        nGram.elements.forEach { out.writeINT(instance.enumerateString(it)) }
    }

    override fun read(`in`: DataInput): NGram {
        val instance = NGramEnumeratingService.getInstance()
        val size = `in`.readINT()
        return NGram((1..size).map { instance.valueOf(`in`.readINT()) })
    }

    override fun isEqual(p0: NGram?, p1: NGram?): Boolean {
        return p0 == p1
    }

    override fun getHashCode(p0: NGram?): Int {
        return p0!!.hashCode()
    }
}

fun DataOutput.writeINT(x : Int) = DataInputOutputUtil.writeINT(this, x)
fun DataInput.readINT() : Int = DataInputOutputUtil.readINT(this)