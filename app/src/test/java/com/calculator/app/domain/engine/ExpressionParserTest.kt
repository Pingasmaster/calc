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
            0,
            BigDecimal(expected).compareTo(actual),
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
    fun `unmatched left paren throws`() {
        val tokens = Tokenizer.tokenize("(1+2")
        assertThrows(IllegalArgumentException::class.java) {
            ExpressionParser.toPostfix(tokens)
        }
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
    fun `percent in addition is percent of left operand`() {
        assertValueEquals("150", evaluate("100+50%"))
    }

    @Test
    fun `percent in subtraction is percent of left operand`() {
        assertValueEquals("80", evaluate("100-20%"))
    }

    @Test
    fun `percent in multiplication is divide by 100`() {
        assertValueEquals("100", evaluate("200*50%"))
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

    // ========== Unary minus combinations ==========

    @Test
    fun `triple unary minus`() {
        assertValueEquals("-5", evaluate("---5"))
    }

    @Test
    fun `unary minus on parenthesized negative`() {
        assertValueEquals("5", evaluate("-(-5)"))
    }

    @Test
    fun `unary minus before multiplication`() {
        assertValueEquals("-6", evaluate("-2*3"))
    }

    @Test
    fun `unary minus on right operand of multiplication`() {
        assertValueEquals("-6", evaluate("2*-3"))
    }

    @Test
    fun `unary minus on both operands`() {
        assertValueEquals("6", evaluate("-2*-3"))
    }

    @Test
    fun `unary minus on parenthesized sum`() {
        assertValueEquals("-5", evaluate("-(2+3)"))
    }

    @Test
    fun `unary minus on pi`() {
        val result = evaluate("-\u03C0")
        assertTrue(result < BigDecimal("-3.14"))
        assertTrue(result > BigDecimal("-3.15"))
    }

    @Test
    fun `unary minus on sqrt`() {
        assertValueEquals("-2", evaluate("-\u221A(4)"))
    }

    @Test
    fun `unary minus before factorial applies after`() {
        // -5! = -(5!) = -120 since factorial binds first via postfix evaluation
        assertValueEquals("-120", evaluate("-5!"))
    }

    // ========== Percent semantics matrix ==========

    @Test
    fun `percent plus percent`() {
        // 50% = 0.5; (0.5) + (0.5 * 50/100) = 0.5 + 0.25 = 0.75
        assertValueEquals("0.75", evaluate("50%+50%"))
    }

    @Test
    fun `chained minus then plus percent`() {
        // 100 - 50% = 50; 50 + 50*20/100 = 60
        assertValueEquals("60", evaluate("100-50%+20%"))
    }

    @Test
    fun `chained plus then minus percent`() {
        // 100 + 50% = 150; 150 - 150*20/100 = 120
        assertValueEquals("120", evaluate("100+50%-20%"))
    }

    @Test
    fun `division by percent divides by decimal`() {
        // 50% as divisor is 0.5, so 200/50% = 400
        assertValueEquals("400", evaluate("200/50%"))
    }

    @Test
    fun `percent times percent`() {
        assertValueEquals("0.25", evaluate("50%*50%"))
    }

    @Test
    fun `stacked percent collapses`() {
        // 50%% = (50/100)/100 = 0.005
        assertValueEquals("0.005", evaluate("50%%"))
    }

    @Test
    fun `percent in parens`() {
        assertValueEquals("0.5", evaluate("(50%)"))
    }

    @Test
    fun `addition of two parenthesized percents`() {
        assertValueEquals("0.75", evaluate("(50%)+(50%)"))
    }

    @Test
    fun `unary minus on percent`() {
        assertValueEquals("-0.5", evaluate("-50%"))
    }

    @Test
    fun `percent of percent expression`() {
        // (100+50%)% = 150% = 1.5 (outer % just divides by 100)
        assertValueEquals("1.5", evaluate("(100+50%)%"))
    }

    // ========== Factorial combinations ==========

    @Test
    fun `factorial of 2`() {
        assertValueEquals("2", evaluate("2!"))
    }

    @Test
    fun `factorial of 3`() {
        assertValueEquals("6", evaluate("3!"))
    }

    @Test
    fun `factorial of 4`() {
        assertValueEquals("24", evaluate("4!"))
    }

    @Test
    fun `factorial of 12`() {
        assertValueEquals("479001600", evaluate("12!"))
    }

    @Test
    fun `factorial of 13`() {
        assertValueEquals("6227020800", evaluate("13!"))
    }

    @Test
    fun `factorial of 20`() {
        assertValueEquals("2432902008176640000", evaluate("20!"))
    }

    @Test
    fun `factorial of parenthesized sum`() {
        assertValueEquals("120", evaluate("(3+2)!"))
    }

    @Test
    fun `factorial of factorial`() {
        // (3!)! = 6! = 720
        assertValueEquals("720", evaluate("(3!)!"))
    }

    @Test
    fun `factorial of sqrt result`() {
        // sqrt(16)! = 4! = 24
        assertValueEquals("24", evaluate("\u221A(16)!"))
    }

    @Test
    fun `factorial divided by number`() {
        assertValueEquals("60", evaluate("5!/2"))
    }

    @Test
    fun `factorial binds tighter than multiplication`() {
        assertValueEquals("12", evaluate("2*3!"))
    }

    @Test
    fun `factorial of 100 is positive and huge`() {
        val result = evaluate("100!")
        assertTrue(result > BigDecimal("1E157"))
    }

    // ========== Sqrt combinations ==========

    @Test
    fun `sqrt of sqrt`() {
        assertEquals(2.0, evaluate("\u221A(\u221A(16))").toDouble(), 0.0001)
    }

    @Test
    fun `sqrt plus sqrt`() {
        assertEquals(8.0, evaluate("\u221A(25)+\u221A(9)").toDouble(), 0.0001)
    }

    @Test
    fun `sqrt of sum`() {
        assertEquals(5.0, evaluate("\u221A(9+16)").toDouble(), 0.0001)
    }

    @Test
    fun `sqrt of pi`() {
        val result = evaluate("\u221A(\u03C0)")
        assertTrue(result > BigDecimal("1.77"))
        assertTrue(result < BigDecimal("1.78"))
    }

    @Test
    fun `sqrt squared is approximately input`() {
        val result = evaluate("\u221A(2)*\u221A(2)")
        assertEquals(2.0, result.toDouble(), 1e-12)
    }

    @Test
    fun `sqrt precision matches 10 places`() {
        val result = evaluate("\u221A(2)")
        // Expect at least 10 decimal places of sqrt(2) accuracy.
        assertEquals(1.41421356237, result.toDouble(), 1e-10)
    }

    // ========== Constants combined ==========

    @Test
    fun `pi minus pi is zero`() {
        assertEquals(0.0, evaluate("\u03C0-\u03C0").toDouble(), 1e-12)
    }

    @Test
    fun `two pi divided by pi is two`() {
        assertEquals(2.0, evaluate("2\u03C0/\u03C0").toDouble(), 1e-12)
    }

    @Test
    fun `e times e`() {
        val result = evaluate("e*e")
        assertTrue(result > BigDecimal("7.38"))
        assertTrue(result < BigDecimal("7.39"))
    }

    @Test
    fun `pi plus e`() {
        val result = evaluate("\u03C0+e")
        assertTrue(result > BigDecimal("5.85"))
        assertTrue(result < BigDecimal("5.86"))
    }

    // ========== Function vs operator precedence ==========

    @Test
    fun `addition then sqrt mirror`() {
        assertValueEquals("3", evaluate("1+\u221A(4)"))
    }

    // ========== Right-associativity flag (synthetic) ==========

    @Test
    fun `right associative operator pushes rather than pops equal precedence`() {
        // No right-assoc operator in prod; construct one to cover the flag branch.
        val tokens = listOf(
            Token.Number("2"),
            Token.Operator("^", 3, isRightAssoc = true),
            Token.Number("3"),
            Token.Operator("^", 3, isRightAssoc = true),
            Token.Number("2"),
        )
        val postfix = ExpressionParser.toPostfix(tokens)
        // Right-assoc should leave second ^ NOT popped before outputting — so the
        // order is operands first, then two ops at the end.
        assertEquals(5, postfix.size)
        assertTrue(postfix[3] is Token.Operator)
        assertTrue(postfix[4] is Token.Operator)
    }

    @Test
    fun `left associative equal precedence pops existing operator`() {
        // Left-assoc (isRightAssoc=false, default) at equal precedence pops.
        val tokens = listOf(
            Token.Number("2"),
            Token.Operator("^", 3, isRightAssoc = false),
            Token.Number("3"),
            Token.Operator("^", 3, isRightAssoc = false),
            Token.Number("2"),
        )
        val postfix = ExpressionParser.toPostfix(tokens)
        // Left-assoc pops earlier ^ before pushing next.
        assertEquals(5, postfix.size)
        // Third element should be an operator (first ^ popped after 3).
        assertTrue(postfix[2] is Token.Operator)
    }

    // ========== Malformed inputs (additional) ==========

    @Test
    fun `stray right paren does not fail evaluation`() {
        // "1+2)" — the parser's final drain skips stray RightParen; expression evaluates.
        assertValueEquals("3", evaluate("1+2)"))
    }

    @Test
    fun `empty parens throws`() {
        assertThrows(ArithmeticException::class.java) {
            evaluate("()")
        }
    }

    @Test
    fun `trailing binary operator throws`() {
        assertThrows(ArithmeticException::class.java) {
            evaluate("1+")
        }
    }

    @Test
    fun `leading binary operator throws`() {
        assertThrows(ArithmeticException::class.java) {
            evaluate("*5")
        }
    }

    @Test
    fun `sqrt with empty parens throws`() {
        assertThrows(ArithmeticException::class.java) {
            evaluate("\u221A()")
        }
    }

    // ========== evaluatePostfix leftover stack ==========

    @Test
    fun `expression leaving two values on stack throws`() {
        val tokens = listOf(
            Token.Number("1"),
            Token.Number("2"),
            Token.Number("3"),
            Token.Operator("+", 1),
        )
        assertThrows(ArithmeticException::class.java) {
            val postfix = ExpressionParser.toPostfix(tokens)
            ExpressionParser.evaluatePostfix(postfix)
        }
    }

    // ========== Numeric precision ==========

    @Test
    fun `BigDecimal adds zero point one and zero point two exactly`() {
        assertValueEquals("0.3", evaluate("0.1+0.2"))
    }

    @Test
    fun `one seventh at fifteen digits precision`() {
        val result = evaluate("1/7")
        // 0.142857142857142857... rounds at 15 sig digits.
        assertTrue(result > BigDecimal("0.142857142857142"))
        assertTrue(result < BigDecimal("0.142857142857144"))
    }

    @Test
    fun `one third times three approximately one`() {
        val result = evaluate("1/3*3")
        assertEquals(1.0, result.toDouble(), 1e-14)
    }
}
