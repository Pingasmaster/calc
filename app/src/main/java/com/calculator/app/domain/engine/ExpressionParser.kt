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
            if (top is Token.LeftParen || top is Token.RightParen) continue
            output.add(top)
        }

        return output
    }

    fun evaluatePostfix(postfix: List<Token>): BigDecimal {
        val stack = ArrayDeque<BigDecimal>()

        for (token in postfix) {
            when (token) {
                is Token.Number -> {
                    stack.addLast(BigDecimal(token.value))
                }

                is Token.Constant -> {
                    stack.addLast(token.value)
                }

                is Token.Operator -> {
                    if (stack.size < 2) throw ArithmeticException("Invalid expression")
                    val b = stack.removeLast()
                    val a = stack.removeLast()
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
                    stack.addLast(result)
                }

                is Token.UnaryMinus -> {
                    if (stack.isEmpty()) throw ArithmeticException("Invalid expression")
                    stack.addLast(stack.removeLast().negate())
                }

                is Token.Percent -> {
                    if (stack.isEmpty()) throw ArithmeticException("Invalid expression")
                    val value = stack.removeLast()
                    stack.addLast(value.divide(BigDecimal(100), MC))
                }

                is Token.Function -> {
                    when (token.name) {
                        "sqrt" -> {
                            if (stack.isEmpty()) throw ArithmeticException("Invalid expression")
                            val value = stack.removeLast()
                            if (value < BigDecimal.ZERO) throw ArithmeticException("Square root of negative number")
                            val sqrtDouble = Math.sqrt(value.toDouble())
                            stack.addLast(BigDecimal(sqrtDouble, MC))
                        }
                        "!" -> {
                            if (stack.isEmpty()) throw ArithmeticException("Invalid expression")
                            val value = stack.removeLast()
                            stack.addLast(factorial(value))
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
        return stack.removeLast()
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
