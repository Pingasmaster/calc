package com.calculator.app.domain.engine

import java.math.BigDecimal

class CalculatorEngine {

    companion object {
        const val MAX_DISPLAY_DECIMAL_PLACES = 10
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
            stripped.toBigInteger().toString()
        } else {
            val plain = stripped.toPlainString()
            // Cap decimal places for display
            if (stripped.scale() > MAX_DISPLAY_DECIMAL_PLACES) {
                value.setScale(MAX_DISPLAY_DECIMAL_PLACES, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString()
            } else {
                plain
            }
        }
    }
}
