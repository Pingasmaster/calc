package com.calculator.app.domain.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object ExpressionParser {

    private const val PRECISION_DIGITS = 15
    private const val MAX_FACTORIAL_INPUT = 170 // 171! exceeds Double.MAX_VALUE
    private val MC = MathContext(PRECISION_DIGITS, RoundingMode.HALF_UP)

    fun toPostfix(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = ArrayDeque<Token>()

        for (token in tokens) {
            when (token) {
                is Token.Number, is Token.Constant -> output.add(token)

                is Token.UnaryMinus -> stack.addLast(token)

                is Token.Function -> {
                    if (token.name == "!") {
                        // Factorial is postfix, apply immediately
                        output.add(token)
                    } else {
                        stack.addLast(token)
                    }
                }

                is Token.Percent -> output.add(token)

                is Token.Operator -> {
                    while (stack.isNotEmpty()) {
                        val top = stack.last()
                        val shouldPop = when {
                            top is Token.Function && top.name != "!" -> true
                            top is Token.UnaryMinus -> true
                            top is Token.Operator -> {
                                if (token.isRightAssoc) top.precedence > token.precedence
                                else top.precedence >= token.precedence
                            }
                            else -> false
                        }
                        if (shouldPop) output.add(stack.removeLast())
                        else break
                    }
                    stack.addLast(token)
                }

                is Token.LeftParen -> stack.addLast(token)

                is Token.RightParen -> {
                    while (stack.isNotEmpty() && stack.last() !is Token.LeftParen) {
                        output.add(stack.removeLast())
                    }
                    if (stack.isNotEmpty() && stack.last() is Token.LeftParen) {
                        stack.removeLast()
                    }
                    // If there's a function on top of the stack, pop it
                    if (stack.isNotEmpty() && stack.last() is Token.Function) {
                        output.add(stack.removeLast())
                    }
                }
            }
        }

        while (stack.isNotEmpty()) {
            val top = stack.removeLast()
            if (top is Token.LeftParen) throw IllegalArgumentException("Unmatched '('")
            if (top is Token.RightParen) continue
            output.add(top)
        }

        return output
    }

    private data class StackVal(val v: BigDecimal, val isPercent: Boolean = false)

    private val HUNDRED = BigDecimal(100)

    private fun StackVal.asDecimal(): BigDecimal =
        if (isPercent) v.divide(HUNDRED, MC) else v

    fun evaluatePostfix(postfix: List<Token>): BigDecimal {
        val stack = ArrayDeque<StackVal>()

        for (token in postfix) {
            when (token) {
                is Token.Number -> stack.addLast(StackVal(BigDecimal(token.value)))

                is Token.Constant -> stack.addLast(StackVal(token.value))

                is Token.Operator -> {
                    if (stack.size < 2) throw ArithmeticException("Invalid expression")
                    val bRaw = stack.removeLast()
                    val a = stack.removeLast().asDecimal()
                    val b = when (token.op) {
                        "+", "-" ->
                            if (bRaw.isPercent) a.multiply(bRaw.v, MC).divide(HUNDRED, MC)
                            else bRaw.v
                        else -> bRaw.asDecimal()
                    }
                    val result = when (token.op) {
                        "+" -> a.add(b, MC)
                        "-" -> a.subtract(b, MC)
                        "*" -> a.multiply(b, MC)
                        "/" -> {
                            if (b.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("Division by zero")
                            a.divide(b, MC)
                        }
                        else -> throw IllegalArgumentException("Unknown operator: ${token.op}")
                    }
                    stack.addLast(StackVal(result))
                }

                is Token.UnaryMinus -> {
                    if (stack.isEmpty()) throw ArithmeticException("Invalid expression")
                    val top = stack.removeLast()
                    stack.addLast(StackVal(top.v.negate(), top.isPercent))
                }

                is Token.Percent -> {
                    if (stack.isEmpty()) throw ArithmeticException("Invalid expression")
                    val top = stack.removeLast()
                    // Collapse stacked percents (e.g. "50%%") into one.
                    val base = if (top.isPercent) top.v.divide(HUNDRED, MC) else top.v
                    stack.addLast(StackVal(base, isPercent = true))
                }

                is Token.Function -> {
                    when (token.name) {
                        "sqrt" -> {
                            if (stack.isEmpty()) throw ArithmeticException("Invalid expression")
                            val value = stack.removeLast().asDecimal()
                            if (value < BigDecimal.ZERO) throw ArithmeticException("Square root of negative number")
                            stack.addLast(StackVal(value.sqrt(MC)))
                        }
                        "!" -> {
                            if (stack.isEmpty()) throw ArithmeticException("Invalid expression")
                            val value = stack.removeLast().asDecimal()
                            stack.addLast(StackVal(factorial(value)))
                        }
                        else -> throw IllegalArgumentException("Unknown function: ${token.name}")
                    }
                }

                is Token.LeftParen, is Token.RightParen -> {
                    // Should not appear in postfix
                }
            }
        }

        if (stack.size != 1) throw ArithmeticException("Invalid expression")
        return stack.removeLast().asDecimal()
    }

    private fun factorial(n: BigDecimal): BigDecimal {
        val intVal = try {
            n.intValueExact()
        } catch (_: ArithmeticException) {
            throw ArithmeticException("Factorial requires a non-negative integer")
        }
        if (intVal < 0) throw ArithmeticException("Factorial of negative number")
        if (intVal > MAX_FACTORIAL_INPUT) throw ArithmeticException("Factorial too large")

        var result = BigDecimal.ONE
        for (i in 2..intVal) {
            result = result.multiply(BigDecimal(i))
        }
        return result
    }
}
