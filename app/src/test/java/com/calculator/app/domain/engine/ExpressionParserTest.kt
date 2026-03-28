package com.calculator.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ExpressionParserTest {

    private fun evaluate(expression: String): BigDecimal {
        val tokens = Tokenizer.tokenize(expression)
        val postfix = ExpressionParser.toPostfix(tokens)
        return ExpressionParser.evaluatePostfix(postfix)
    }

    private fun assertValueEquals(expected: String, actual: BigDecimal) {
        assertEquals(
            "Expected $expected but was ${actual.toPlainString()}",
            0, BigDecimal(expected).compareTo(actual),
        )
    }

    // ========== Basic arithmetic ==========

    @Test
    fun `addition of two numbers`() {
        assertValueEquals("5", evaluate("2+3"))
    }

    @Test
    fun `subtraction of two numbers`() {
        assertValueEquals("2", evaluate("5-3"))
    }

    @Test
    fun `multiplication of two numbers`() {
        assertValueEquals("15", evaluate("3*5"))
    }

    @Test
    fun `division of two numbers`() {
        assertValueEquals("4", evaluate("8/2"))
    }

    @Test
    fun `decimal division`() {
        val result = evaluate("1/3")
        assertTrue(result > BigDecimal("0.333"))
        assertTrue(result < BigDecimal("0.334"))
    }

    // ========== Operator precedence ==========

    @Test
    fun `multiplication before addition`() {
        assertValueEquals("14", evaluate("2+3*4"))
    }

    @Test
    fun `multiplication before subtraction`() {
        assertValueEquals("-10", evaluate("2-3*4"))
    }

    @Test
    fun `division before addition`() {
        assertValueEquals("4", evaluate("2+8/4"))
    }

    @Test
    fun `same precedence operators left to right`() {
        assertValueEquals("9", evaluate("10-3+2"))
    }

    @Test
    fun `multiplication and division left to right`() {
        assertValueEquals("8", evaluate("12/3*2"))
    }

    // ========== Parentheses ==========

    @Test
    fun `parentheses override precedence`() {
        assertValueEquals("20", evaluate("(2+3)*4"))
    }

    @Test
    fun `nested parentheses`() {
        assertValueEquals("21", evaluate("((2+1)*(3+4))"))
    }

    @Test
    fun `deeply nested parentheses`() {
        assertValueEquals("5", evaluate("(((((5)))))"))
    }

    @Test
    fun `parentheses with subtraction`() {
        assertValueEquals("6", evaluate("(10-4)"))
    }

    @Test
    fun `unmatched left paren is tolerated`() {
        val tokens = Tokenizer.tokenize("(1+2")
        val postfix = ExpressionParser.toPostfix(tokens)
        val result = ExpressionParser.evaluatePostfix(postfix)
        assertValueEquals("3", result)
    }

    // ========== Unary minus ==========

    @Test
    fun `unary minus on single number`() {
        assertValueEquals("-5", evaluate("-5"))
    }

    @Test
    fun `unary minus in parentheses`() {
        assertValueEquals("-3", evaluate("(-3)"))
    }

    @Test
    fun `unary minus with addition`() {
        assertValueEquals("-2", evaluate("-5+3"))
    }

    @Test
    fun `double negative via operator then unary`() {
        assertValueEquals("8", evaluate("3+-5+10"))
    }

    @Test
    fun `unary minus in nested expression`() {
        assertValueEquals("-6", evaluate("(-2)*3"))
    }

    // ========== Division by zero ==========

    @Test
    fun `division by zero throws ArithmeticException`() {
        assertThrows(ArithmeticException::class.java) {
            evaluate("1/0")
        }
    }

    @Test
    fun `division by zero in complex expression`() {
        assertThrows(ArithmeticException::class.java) {
            evaluate("5+3/0")
        }
    }

    // ========== Percent ==========

    @Test
    fun `basic percent`() {
        assertValueEquals("0.5", evaluate("50%"))
    }

    @Test
    fun `percent in expression`() {
        assertValueEquals("100.5", evaluate("100+50%"))
    }

    @Test
    fun `one hundred percent`() {
        assertValueEquals("1", evaluate("100%"))
    }

    @Test
    fun `percent of zero`() {
        assertValueEquals("0", evaluate("0%"))
    }

    // ========== Square root ==========

    @Test
    fun `sqrt of perfect square`() {
        assertEquals(3.0, evaluate("\u221A(9)").toDouble(), 0.0001)
    }

    @Test
    fun `sqrt of 0`() {
        assertEquals(0.0, evaluate("\u221A(0)").toDouble(), 0.0001)
    }

    @Test
    fun `sqrt of 1`() {
        assertEquals(1.0, evaluate("\u221A(1)").toDouble(), 0.0001)
    }

    @Test
    fun `sqrt of 2`() {
        assertEquals(1.4142, evaluate("\u221A(2)").toDouble(), 0.001)
    }

    @Test
    fun `sqrt of negative number throws`() {
        assertThrows(ArithmeticException::class.java) {
            evaluate("\u221A(-1)")
        }
    }

    @Test
    fun `sqrt in expression`() {
        assertEquals(3.0, evaluate("\u221A(4)+1").toDouble(), 0.0001)
    }

    // ========== Factorial ==========

    @Test
    fun `factorial of 0`() {
        assertValueEquals("1", evaluate("0!"))
    }

    @Test
    fun `factorial of 1`() {
        assertValueEquals("1", evaluate("1!"))
    }

    @Test
    fun `factorial of 5`() {
        assertValueEquals("120", evaluate("5!"))
    }

    @Test
    fun `factorial of 10`() {
        assertValueEquals("3628800", evaluate("10!"))
    }

    @Test
    fun `factorial of negative throws`() {
        assertThrows(ArithmeticException::class.java) {
            evaluate("(-1)!")
        }
    }

    @Test
    fun `factorial of non-integer throws`() {
        assertThrows(ArithmeticException::class.java) {
            evaluate("2.5!")
        }
    }

    @Test
    fun `factorial too large throws`() {
        assertThrows(ArithmeticException::class.java) {
            evaluate("171!")
        }
    }

    @Test
    fun `factorial at max boundary`() {
        val result = evaluate("170!")
        assertTrue(result > BigDecimal.ZERO)
    }

    @Test
    fun `factorial in expression`() {
        assertValueEquals("7", evaluate("3!+1"))
    }

    // ========== Constants in expressions ==========

    @Test
    fun `pi plus 1`() {
        val result = evaluate("\u03C0+1")
        assertTrue(result > BigDecimal("4.14"))
        assertTrue(result < BigDecimal("4.15"))
    }

    @Test
    fun `e plus 1`() {
        val result = evaluate("e+1")
        assertTrue(result > BigDecimal("3.71"))
        assertTrue(result < BigDecimal("3.72"))
    }

    @Test
    fun `2 times pi (implicit multiply)`() {
        val result = evaluate("2\u03C0")
        assertTrue(result > BigDecimal("6.28"))
        assertTrue(result < BigDecimal("6.29"))
    }

    // ========== Complex expressions ==========

    @Test
    fun `order of operations complex`() {
        assertValueEquals("11", evaluate("2+3*4-6/2"))
    }

    @Test
    fun `parentheses in complex expression`() {
        assertValueEquals("15", evaluate("(2+3)*(4-1)"))
    }

    @Test
    fun `multiple operations with large numbers`() {
        assertValueEquals("1000000", evaluate("1000*1000"))
    }

    @Test
    fun `chained additions`() {
        assertValueEquals("15", evaluate("1+2+3+4+5"))
    }

    @Test
    fun `chained multiplications`() {
        assertValueEquals("120", evaluate("1*2*3*4*5"))
    }

    @Test
    fun `single number`() {
        assertValueEquals("42", evaluate("42"))
    }

    // ========== Invalid expressions ==========

    @Test
    fun `operator with insufficient operands throws`() {
        assertThrows(ArithmeticException::class.java) {
            evaluate("+")
        }
    }

    @Test
    fun `percent on empty stack throws`() {
        assertThrows(ArithmeticException::class.java) {
            val tokens = listOf(Token.Percent)
            val postfix = ExpressionParser.toPostfix(tokens)
            ExpressionParser.evaluatePostfix(postfix)
        }
    }

    @Test
    fun `unary minus on empty stack throws`() {
        assertThrows(ArithmeticException::class.java) {
            val tokens = listOf(Token.UnaryMinus())
            val postfix = ExpressionParser.toPostfix(tokens)
            ExpressionParser.evaluatePostfix(postfix)
        }
    }

    @Test
    fun `sqrt on empty stack throws`() {
        assertThrows(ArithmeticException::class.java) {
            val tokens = listOf(Token.Function("sqrt"))
            val postfix = ExpressionParser.toPostfix(tokens)
            ExpressionParser.evaluatePostfix(postfix)
        }
    }

    @Test
    fun `factorial on empty stack throws`() {
        assertThrows(ArithmeticException::class.java) {
            val tokens = listOf(Token.Function("!"))
            val postfix = ExpressionParser.toPostfix(tokens)
            ExpressionParser.evaluatePostfix(postfix)
        }
    }

    @Test
    fun `unknown function throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            val tokens = listOf(Token.Number("5"), Token.Function("unknown"))
            val postfix = ExpressionParser.toPostfix(tokens)
            ExpressionParser.evaluatePostfix(postfix)
        }
    }

    @Test
    fun `unknown operator throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            val tokens = listOf(
                Token.Number("5"),
                Token.Number("3"),
                Token.Operator("^", 3),
            )
            val postfix = ExpressionParser.toPostfix(tokens)
            ExpressionParser.evaluatePostfix(postfix)
        }
    }

    // ========== Postfix conversion correctness ==========

    @Test
    fun `toPostfix simple addition`() {
        val tokens = Tokenizer.tokenize("1+2")
        val postfix = ExpressionParser.toPostfix(tokens)
        assertEquals(3, postfix.size)
        assertTrue(postfix[0] is Token.Number)
        assertTrue(postfix[1] is Token.Number)
        assertTrue(postfix[2] is Token.Operator)
    }

    @Test
    fun `toPostfix respects precedence`() {
        val tokens = Tokenizer.tokenize("1+2*3")
        val postfix = ExpressionParser.toPostfix(tokens)
        assertEquals(5, postfix.size)
        assertTrue(postfix[3] is Token.Operator)
        assertEquals("*", (postfix[3] as Token.Operator).op)
        assertTrue(postfix[4] is Token.Operator)
        assertEquals("+", (postfix[4] as Token.Operator).op)
    }

    @Test
    fun `toPostfix with parentheses`() {
        val tokens = Tokenizer.tokenize("(1+2)*3")
        val postfix = ExpressionParser.toPostfix(tokens)
        assertEquals(5, postfix.size)
        assertEquals("+", (postfix[2] as Token.Operator).op)
        assertEquals("*", (postfix[4] as Token.Operator).op)
    }

    @Test
    fun `toPostfix factorial is immediately output`() {
        val tokens = Tokenizer.tokenize("5!")
        val postfix = ExpressionParser.toPostfix(tokens)
        assertEquals(2, postfix.size)
        assertTrue(postfix[0] is Token.Number)
        assertTrue(postfix[1] is Token.Function)
    }

    @Test
    fun `toPostfix function after right paren is popped`() {
        val tokens = Tokenizer.tokenize("\u221A(4)")
        val postfix = ExpressionParser.toPostfix(tokens)
        assertTrue(postfix.last() is Token.Function)
        assertEquals("sqrt", (postfix.last() as Token.Function).name)
    }

    @Test
    fun `expression resulting in non-single stack throws`() {
        assertThrows(ArithmeticException::class.java) {
            val tokens = listOf(Token.Number("1"), Token.Number("2"))
            val postfix = ExpressionParser.toPostfix(tokens)
            ExpressionParser.evaluatePostfix(postfix)
        }
    }
}
