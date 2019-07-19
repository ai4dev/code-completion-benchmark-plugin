package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.tokenization.tokenizers

import com.intellij.lang.java.lexer.JavaLexer
import com.intellij.pom.java.LanguageLevel
import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.io.Reader
import java.util.ArrayList

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.tokenization.Tokenizer
import java.io.File

class JavaTokenizer : Tokenizer {

    private val lexer = JavaLexer(LanguageLevel.JDK_1_8)

    override fun tokenizeLine(line: String): Sequence<String> {
        return tokenizeLines(line)[0].asSequence()
    }

    override fun tokenizeText(text: String): Sequence<Sequence<String>> {
        return tokenizeLines(text).asSequence().map{ it.asSequence() }
    }

    override fun tokenizeFile(file: File): Sequence<Sequence<String>> {
        return tokenizeText(Reader.readLines(file).joinToString("\n"))
    }

    fun tokenizeLines(text: CharSequence): List<List<String>> {
        lexer.start(text)
        val lineTokens = ArrayList<MutableList<String>>()
        val tokens = ArrayList<String>()
        lineTokens.add(ArrayList())

        while (lexer.tokenType != null) {
            var tokenText = lexer.tokenText

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

            lexer.advance()
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
            return JavaLexer.isKeyword(token, LanguageLevel.JDK_1_8)
        }
    }
}