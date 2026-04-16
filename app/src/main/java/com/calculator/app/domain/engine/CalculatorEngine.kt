package com.calculator.app.domain.engine

import java.math.BigDecimal

class CalculatorEngine {

    companion object {
        const val MAX_DISPLAY_DECIMAL_PLACES = 10
        private const val MAX_INTEGER_DIGITS = 15
    }

    fun evaluate(expression: String): Result<String> {
        if (expression.isBlank()) return Result.failure(IllegalArgumentException("Empty expression"))
        return try {
            val tokens = Tokenizer.tokenize(expression)
            if (tokens.isEmpty()) return Result.failure(IllegalArgumentException("No valid tokens"))
            val postfix = ExpressionParser.toPostfix(tokens)
            val result = ExpressionParser.evaluatePostfix(postfix)
            Result.success(formatResult(result))
        } catch (e: ArithmeticException) {
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ArithmeticException("Error"))
        }
    }

    private fun formatResult(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        return if (stripped.scale() <= 0) {
            val intStr = stripped.toBigInteger().toString()
            if (intStr.replace("-", "").length > MAX_INTEGER_DIGITS) {
                stripped.round(java.math.MathContext(MAX_DISPLAY_DECIMAL_PLACES + 1))
                    .stripTrailingZeros()
                    .toString()
            } else {
                intStr
            }
        } else {
            val plain = stripped.toPlainString()
            // Cap decimal places for display
            if (stripped.scale() > MAX_DISPLAY_DECIMAL_PLACES) {
                stripped.setScale(MAX_DISPLAY_DECIMAL_PLACES, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
            } else {
                plain
            }
        }
    }
}
