package com.calculator.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TokenizerTest {

    // ========== Basic number tokenization ==========

    @Test
    fun `tokenize single digit`() {
        val tokens = Tokenizer.tokenize("5")
        assertEquals(1, tokens.size)
        assertEquals(Token.Number("5"), tokens[0])
    }

    @Test
    fun `tokenize multi-digit number`() {
        val tokens = Tokenizer.tokenize("123")
        assertEquals(1, tokens.size)
        assertEquals(Token.Number("123"), tokens[0])
    }

    @Test
    fun `tokenize decimal number`() {
        val tokens = Tokenizer.tokenize("3.14")
        assertEquals(1, tokens.size)
        assertEquals(Token.Number("3.14"), tokens[0])
    }

    @Test
    fun `tokenize number starting with decimal point`() {
        val tokens = Tokenizer.tokenize(".5")
        // .5 needs a digit after the dot to be parsed as number
        assertEquals(1, tokens.size)
        assertEquals(Token.Number(".5"), tokens[0])
    }

    @Test
    fun `tokenize number with leading zero`() {
        val tokens = Tokenizer.tokenize("007")
        assertEquals(1, tokens.size)
        assertEquals(Token.Number("007"), tokens[0])
    }

    // ========== Operator tokenization ==========

    @Test
    fun `tokenize addition`() {
        val tokens = Tokenizer.tokenize("1+2")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("+", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `tokenize subtraction`() {
        val tokens = Tokenizer.tokenize("5-3")
        assertEquals(3, tokens.size)
        assertEquals("-", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `tokenize multiplication with unicode symbol`() {
        val tokens = Tokenizer.tokenize("2\u00D73")
        assertEquals(3, tokens.size)
        assertEquals("*", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `tokenize division with unicode symbol`() {
        val tokens = Tokenizer.tokenize("6\u00F72")
        assertEquals(3, tokens.size)
        assertEquals("/", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `tokenize minus with unicode symbol`() {
        val tokens = Tokenizer.tokenize("5\u22122")
        assertEquals(3, tokens.size)
        assertEquals("-", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `operator precedence - addition is 1`() {
        val tokens = Tokenizer.tokenize("1+2")
        assertEquals(1, (tokens[1] as Token.Operator).precedence)
    }

    @Test
    fun `operator precedence - subtraction is 1`() {
        val tokens = Tokenizer.tokenize("1-2")
        // first '-' is binary operator because preceded by number
        // Actually, "1-2" -> Number(1), Operator("-", 1), Number(2)
        assertEquals(1, (tokens[1] as Token.Operator).precedence)
    }

    @Test
    fun `operator precedence - multiplication is 2`() {
        val tokens = Tokenizer.tokenize("1*2")
        assertEquals(2, (tokens[1] as Token.Operator).precedence)
    }

    @Test
    fun `operator precedence - division is 2`() {
        val tokens = Tokenizer.tokenize("1/2")
        assertEquals(2, (tokens[1] as Token.Operator).precedence)
    }

    // ========== Unary minus ==========

    @Test
    fun `unary minus at start of expression`() {
        val tokens = Tokenizer.tokenize("-5")
        assertEquals(2, tokens.size)
        assertTrue(tokens[0] is Token.UnaryMinus)
        assertEquals(Token.Number("5"), tokens[1])
    }

    @Test
    fun `unary minus after left paren`() {
        val tokens = Tokenizer.tokenize("(-5)")
        assertTrue(tokens[1] is Token.UnaryMinus)
    }

    @Test
    fun `unary minus after operator`() {
        val tokens = Tokenizer.tokenize("3+-5")
        assertEquals(4, tokens.size)
        assertTrue(tokens[0] is Token.Number)
        assertTrue(tokens[1] is Token.Operator)
        assertTrue(tokens[2] is Token.UnaryMinus)
        assertTrue(tokens[3] is Token.Number)
    }

    @Test
    fun `binary minus after number`() {
        val tokens = Tokenizer.tokenize("5-3")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("-", (tokens[1] as Token.Operator).op)
    }

    // ========== Constants ==========

    @Test
    fun `tokenize pi constant`() {
        val tokens = Tokenizer.tokenize("\u03C0")
        assertEquals(1, tokens.size)
        assertTrue(tokens[0] is Token.Constant)
        assertEquals("\u03C0", (tokens[0] as Token.Constant).name)
    }

    @Test
    fun `tokenize e constant`() {
        val tokens = Tokenizer.tokenize("e")
        assertEquals(1, tokens.size)
        assertTrue(tokens[0] is Token.Constant)
        assertEquals("e", (tokens[0] as Token.Constant).name)
    }

    @Test
    fun `pi value is approximately correct`() {
        val tokens = Tokenizer.tokenize("\u03C0")
        val value = (tokens[0] as Token.Constant).value
        assertTrue(value > BigDecimal("3.14"))
        assertTrue(value < BigDecimal("3.15"))
    }

    @Test
    fun `e value is approximately correct`() {
        val tokens = Tokenizer.tokenize("e")
        val value = (tokens[0] as Token.Constant).value
        assertTrue(value > BigDecimal("2.71"))
        assertTrue(value < BigDecimal("2.72"))
    }

    // ========== Parentheses ==========

    @Test
    fun `tokenize left paren`() {
        val tokens = Tokenizer.tokenize("(1)")
        assertTrue(tokens[0] is Token.LeftParen)
    }

    @Test
    fun `tokenize right paren`() {
        val tokens = Tokenizer.tokenize("(1)")
        assertTrue(tokens[2] is Token.RightParen)
    }

    @Test
    fun `nested parentheses`() {
        val tokens = Tokenizer.tokenize("((1+2))")
        val leftParens = tokens.count { it is Token.LeftParen }
        val rightParens = tokens.count { it is Token.RightParen }
        assertEquals(2, leftParens)
        assertEquals(2, rightParens)
    }

    // ========== Functions ==========

    @Test
    fun `tokenize sqrt function`() {
        val tokens = Tokenizer.tokenize("\u221A(4)")
        assertTrue(tokens[0] is Token.Function)
        assertEquals("sqrt", (tokens[0] as Token.Function).name)
    }

    @Test
    fun `tokenize factorial`() {
        val tokens = Tokenizer.tokenize("5!")
        assertEquals(2, tokens.size)
        assertTrue(tokens[1] is Token.Function)
        assertEquals("!", (tokens[1] as Token.Function).name)
    }

    // ========== Percent ==========

    @Test
    fun `tokenize percent`() {
        val tokens = Tokenizer.tokenize("50%")
        assertEquals(2, tokens.size)
        assertTrue(tokens[1] is Token.Percent)
    }

    // ========== Implicit multiplication ==========

    @Test
    fun `implicit multiply between number and left paren`() {
        val tokens = Tokenizer.tokenize("2(3)")
        // Should be: Number(2), Operator(*), LeftParen, Number(3), RightParen
        assertEquals(5, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("*", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `implicit multiply between right paren and number`() {
        val tokens = Tokenizer.tokenize("(3)2")
        // Should be: LeftParen, Number(3), RightParen, Operator(*), Number(2)
        assertEquals(5, tokens.size)
        assertTrue(tokens[3] is Token.Operator)
        assertEquals("*", (tokens[3] as Token.Operator).op)
    }

    @Test
    fun `implicit multiply between right paren and left paren`() {
        val tokens = Tokenizer.tokenize("(2)(3)")
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("*", operators[0].op)
    }

    @Test
    fun `implicit multiply between number and pi`() {
        val tokens = Tokenizer.tokenize("2\u03C0")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("*", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `implicit multiply between number and e`() {
        val tokens = Tokenizer.tokenize("2e")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("*", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `implicit multiply between pi and number`() {
        val tokens = Tokenizer.tokenize("\u03C02")
        assertEquals(3, tokens.size)
        assertTrue(tokens[1] is Token.Operator)
        assertEquals("*", (tokens[1] as Token.Operator).op)
    }

    @Test
    fun `implicit multiply between constant and left paren`() {
        val tokens = Tokenizer.tokenize("\u03C0(2)")
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("*", operators[0].op)
    }

    @Test
    fun `implicit multiply between right paren and constant`() {
        val tokens = Tokenizer.tokenize("(2)\u03C0")
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("*", operators[0].op)
    }

    @Test
    fun `implicit multiply between percent and number`() {
        val tokens = Tokenizer.tokenize("50%2")
        // 50, %, *, 2
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("*", operators[0].op)
    }

    @Test
    fun `implicit multiply between factorial and number`() {
        val tokens = Tokenizer.tokenize("5!2")
        // 5, !, *, 2
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("*", operators[0].op)
    }

    @Test
    fun `implicit multiply between number and sqrt`() {
        val tokens = Tokenizer.tokenize("2\u221A(4)")
        // 2, *, sqrt, (, 4, )
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("*", operators[0].op)
    }

    @Test
    fun `no implicit multiply between operator and number`() {
        val tokens = Tokenizer.tokenize("2+3")
        assertEquals(3, tokens.size)
        // Only one operator, the +
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(1, operators.size)
        assertEquals("+", operators[0].op)
    }

    // ========== Whitespace handling ==========

    @Test
    fun `whitespace is ignored`() {
        val tokens = Tokenizer.tokenize("1 + 2")
        assertEquals(3, tokens.size)
    }

    // ========== Empty and edge cases ==========

    @Test
    fun `empty string produces empty token list`() {
        val tokens = Tokenizer.tokenize("")
        assertTrue(tokens.isEmpty())
    }

    @Test
    fun `whitespace only produces empty token list`() {
        val tokens = Tokenizer.tokenize("   ")
        assertTrue(tokens.isEmpty())
    }

    @Test
    fun `unknown characters throw`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize("1@2")
        }
    }

    // ========== Complex expressions ==========

    @Test
    fun `complex expression tokenization`() {
        val tokens = Tokenizer.tokenize("2+3\u00D74")
        assertEquals(5, tokens.size)
        assertEquals(Token.Number("2"), tokens[0])
        assertEquals("+", (tokens[1] as Token.Operator).op)
        assertEquals(Token.Number("3"), tokens[2])
        assertEquals("*", (tokens[3] as Token.Operator).op)
        assertEquals(Token.Number("4"), tokens[4])
    }

    @Test
    fun `expression with all operator types`() {
        val tokens = Tokenizer.tokenize("1+2-3*4/5")
        val operators = tokens.filterIsInstance<Token.Operator>()
        assertEquals(4, operators.size)
        assertEquals("+", operators[0].op)
        assertEquals("-", operators[1].op)
        assertEquals("*", operators[2].op)
        assertEquals("/", operators[3].op)
    }

    @Test
    fun `expression with decimal at end of number`() {
        // A lone "3." should tokenize as Number("3") since no digit follows the dot
        val tokens = Tokenizer.tokenize("3.")
        // Trailing dot is stripped during tokenization.
        assertEquals(1, tokens.size)
        assertEquals(Token.Number("3"), tokens[0])
    }

    @Test
    fun `consecutive unary and binary operators`() {
        // "1*-2" should produce: Number(1), Operator(*), UnaryMinus, Number(2)
        val tokens = Tokenizer.tokenize("1*-2")
        assertEquals(4, tokens.size)
        assertTrue(tokens[2] is Token.UnaryMinus)
    }

    @Test
    fun `e followed by letter throws`() {
        // "ex" - 'e' followed by a letter is not tokenized as the constant 'e',
        // and the lookahead leaves both chars as unknown → Tokenizer rejects it.
        assertThrows(IllegalArgumentException::class.java) {
            Tokenizer.tokenize("ex")
        }
    }

    @Test
    fun `e at end of expression is a constant`() {
        val tokens = Tokenizer.tokenize("e")
        assertEquals(1, tokens.size)
        assertTrue(tokens[0] is Token.Constant)
    }

    @Test
    fun `e followed by operator is a constant`() {
        val tokens = Tokenizer.tokenize("e+1")
        assertTrue(tokens[0] is Token.Constant)
    }
}
