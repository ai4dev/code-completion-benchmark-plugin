package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.lang.tokenizers

import com.intellij.lang.ASTNode
import com.intellij.lang.java.lexer.JavaLexer
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiFile

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.lang.Tokenizer
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.ngram.lang.collectors.JavaASTCollector

import java.util.*

class JavaTokenizer : Tokenizer {

    private val tokenCollector = JavaASTCollector

    override fun tokenizeFile(file: PsiFile): Sequence<Sequence<String>> {
        return itemsToLines(tokenCollector.getElements(file))
                .asSequence()
                .map { it.asSequence() }
    }

    override fun itemsToLines(items: List<ASTNode>): List<List<String>> {
        val lineTokens = ArrayList<MutableList<String>>()
        val tokens = ArrayList<String>()
        lineTokens.add(ArrayList())

        for (item in items) {
            var tokenText = item.text
            if (tokenText.contains(System.lineSeparator())) {
                lineTokens.add(ArrayList())
            }

            if (tokenText.startsWith("\"") && tokenText.endsWith("\"") && tokenText.length > 2) {
                if (tokenText.length >= 15) {
                    tokenText = "\"\""
                } else {
                    var body = tokenText.substring(1, tokenText.length - 1)
                    body = body.replace("\\\\".toRegex(), "\\\\\\\\")
                    body = body.replace("\"".toRegex(), "\\\\\"")
                    body = body.replace("\n".toRegex(), "\\n")
                    body = body.replace("\r".toRegex(), "\\r")
                    body = body.replace("\t".toRegex(), "\\t")
                    tokenText = "\"" + body + "\""
                }
            } else if (tokenText.startsWith("\'") && tokenText.endsWith("\'")) {
                tokenText = tokenText.replace("\n".toRegex(), "\\n")
                tokenText = tokenText.replace("\r".toRegex(), "\\r")
                tokenText = tokenText.replace("\t".toRegex(), "\\t")
            } else if (tokenText.matches(">>+".toRegex())) {
                var split = false
                for (i in tokens.indices.reversed()) {
                    val token = tokens[i]
                    if (token.matches("[,\\.\\?\\[\\]]".toRegex()) || Character.isUpperCase(token[0])
                            || token == "extends" || token == "super"
                            || token.matches("(byte|short|int|long|float|double)".toRegex())) {
                        continue
                    } else if (token.matches("(<|>)+".toRegex())) {
                        split = true
                        break
                    } else {
                        break
                    }
                }
                if (split) {
                    for (i in 0 until tokenText.length) {
                        tokens.add(">")
                        lineTokens[lineTokens.size - 1].add(">")
                    }
                    continue
                }
            }

            tokens.add(tokenText)
            lineTokens[lineTokens.size - 1].add(tokenText)
        }
        return lineTokens
    }

    companion object {

        private const val ID_REGEX = "[a-zA-Z_$][a-zA-Z\\d_$]*"
        private const val HEX_REGEX = "0x([0-9a-fA-F]+_)*[0-9a-fA-F]+[lLfFdD]?"
        private const val BIN_REGEX = "0b([01]+_)*[01]+[lL]"
        private const val IR_REGEX = "([0-9]+_)*[0-9]+[lLfFdD]?"
        // A: nrs before and after dot, B: nrs only before dot, C nrs only after, D: only E as indicator
        private const val DBL_REGEXA = "[0-9]+\\.[0-9]+([eE][-+]?[0-9]+)?[fFdD]?"
        private const val DBL_REGEXB = "[0-9]+\\.([eE][-+]?[0-9]+)?[fFdD]?"
        private const val DBL_REGEXC = "\\.[0-9]+([eE][-+]?[0-9]+)?[fFdD]?"
        private const val DBL_REGEXD = "[0-9]+[eE][-+]?[0-9]+[fFdD]?"

        fun isID(token: String): Boolean {
            return !isKeyword(token) && token.matches(ID_REGEX.toRegex())
        }

        fun isNR(token: String): Boolean {
            return token.matches(("(" + HEX_REGEX + "|" + IR_REGEX + "|" + BIN_REGEX +
                    "|" + DBL_REGEXA + "|" + DBL_REGEXB + "|" + DBL_REGEXC + "|" + DBL_REGEXD + ")").toRegex())
        }

        fun isSTR(token: String): Boolean {
            return token.matches("\".+\"".toRegex())
        }

        fun isChar(token: String): Boolean {
            return token.matches("'.+'".toRegex())
        }

        fun isKeyword(token: String): Boolean {
            return JavaLexer.isKeyword(token, LanguageLevel.HIGHEST)
        }
    }
}