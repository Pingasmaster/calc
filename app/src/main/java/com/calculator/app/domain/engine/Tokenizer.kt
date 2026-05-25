package com.calculator.app.domain.engine

import java.math.BigDecimal

sealed class Token {
    /**
     * Numeric literal. `value` is the original digit string (preserved for
     * equality and round-tripping); `decimal` is parsed once at construction
     * so postfix evaluation doesn't re-parse the literal on every preview tick.
     */
    data class Number(val value: String) : Token() {
        val decimal: BigDecimal = BigDecimal(value)
    }
    data class Operator(val op: String, val precedence: Int, val isRightAssoc: Boolean = false) : Token()
    data class Function(val name: String) : Token()
    data class Constant(val name: String, val value: BigDecimal) : Token()
    data object LeftParen : Token()
    data object RightParen : Token()
    data object Percent : Token()
    data class UnaryMinus(val precedence: Int = 3) : Token()
}

object Tokenizer {

    private val PI = BigDecimal("3.141592653589793238462643")
    private val E = BigDecimal("2.718281828459045235360287")

    private const val U_TIMES = '×' // ×
    private const val U_DIV = '÷' // ÷
    private const val U_MINUS = '−' // −

    fun tokenize(expression: String): List<Token> {
        // Skip the three replace() passes (= three string allocations) when the
        // input contains no Unicode operator glyphs — the dominant ASCII case.
        val needsNormalize = expression.any { it == U_TIMES || it == U_DIV || it == U_MINUS }
        val expr = if (needsNormalize) {
            expression
                .replace(U_TIMES, '*')
                .replace(U_DIV, '/')
                .replace(U_MINUS, '-')
        } else {
            expression
        }

        val tokens = mutableListOf<Token>()
        var i = 0

        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++

                c.isDigit() || (c == '.' && i + 1 < expr.length && expr[i + 1].isDigit()) -> {
                    val start = i
                    var hasDecimal = false
                    while (i < expr.length && (expr[i].isDigit() || (expr[i] == '.' && !hasDecimal))) {
                        if (expr[i] == '.') hasDecimal = true
                        i++
                    }
                    if (i < expr.length && expr[i] == '.') {
                        throw IllegalArgumentException("Invalid number format")
                    }
                    var numStr = expr.substring(start, i)
                    if (numStr.endsWith(".")) numStr = numStr.dropLast(1)
                    // Insert implicit multiplication if preceded by a closing paren, constant, or number
                    if (tokens.isNotEmpty() && needsImplicitMultiply(tokens.last())) {
                        tokens.add(Token.Operator("*", 2))
                    }
                    tokens.add(Token.Number(numStr))
                }

                c == 'π' -> {
                    if (tokens.isNotEmpty() && needsImplicitMultiply(tokens.last())) {
                        tokens.add(Token.Operator("*", 2))
                    }
                    tokens.add(Token.Constant("π", PI))
                    i++
                }

                c == 'e' && (i + 1 >= expr.length || !expr[i + 1].isLetter()) -> {
                    if (tokens.isNotEmpty() && needsImplicitMultiply(tokens.last())) {
                        tokens.add(Token.Operator("*", 2))
                    }
                    tokens.add(Token.Constant("e", E))
                    i++
                }

                c == '+' -> {
                    tokens.add(Token.Operator("+", 1))
                    i++
                }

                c == '-' -> {
                    // Determine if this is unary minus
                    val last = tokens.lastOrNull()
                    if (last == null || last is Token.LeftParen || last is Token.Operator || last is Token.UnaryMinus) {
                        tokens.add(Token.UnaryMinus())
                    } else {
                        tokens.add(Token.Operator("-", 1))
                    }
                    i++
                }

                c == '*' -> {
                    tokens.add(Token.Operator("*", 2))
                    i++
                }

                c == '/' -> {
                    tokens.add(Token.Operator("/", 2))
                    i++
                }

                c == '%' -> {
                    tokens.add(Token.Percent)
                    i++
                }

                c == '!' -> {
                    tokens.add(Token.Function("!"))
                    i++
                }

                c == '(' -> {
                    if (tokens.isNotEmpty() && needsImplicitMultiply(tokens.last())) {
                        tokens.add(Token.Operator("*", 2))
                    }
                    tokens.add(Token.LeftParen)
                    i++
                }

                c == ')' -> {
                    tokens.add(Token.RightParen)
                    i++
                }

                c == '√' -> {
                    if (tokens.isNotEmpty() && needsImplicitMultiply(tokens.last())) {
                        tokens.add(Token.Operator("*", 2))
                    }
                    tokens.add(Token.Function("sqrt"))
                    i++
                }

                else -> throw IllegalArgumentException("Unexpected character '$c'")
            }
        }
        return tokens
    }

    private fun needsImplicitMultiply(token: Token): Boolean = token is Token.Number ||
        token is Token.Constant ||
        token is Token.RightParen ||
        token is Token.Percent ||
        (token is Token.Function && token.name == "!")
}
