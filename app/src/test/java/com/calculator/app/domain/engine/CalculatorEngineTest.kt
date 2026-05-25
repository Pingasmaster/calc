package com.calculator.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

    private val engine = CalculatorEngine()

    // ========== Basic evaluation ==========

    @Test
    fun `evaluate simple addition`() {
        val result = engine.evaluate("1+2")
        assertTrue(result.isSuccess)
        assertEquals("3", result.getOrNull())
    }

    @Test
    fun `evaluate simple subtraction`() {
        val result = engine.evaluate("10\u22125")
        assertTrue(result.isSuccess)
        assertEquals("5", result.getOrNull())
    }

    @Test
    fun `evaluate simple multiplication`() {
        val result = engine.evaluate("4\u00D73")
        assertTrue(result.isSuccess)
        assertEquals("12", result.getOrNull())
    }

    @Test
    fun `evaluate simple division`() {
        val result = engine.evaluate("15\u00F75")
        assertTrue(result.isSuccess)
        assertEquals("3", result.getOrNull())
    }

    // ========== Result formatting ==========

    @Test
    fun `integer result has no decimal`() {
        val result = engine.evaluate("2+3")
        assertEquals("5", result.getOrNull())
    }

    @Test
    fun `decimal result preserves needed decimals`() {
        val result = engine.evaluate("1/4")
        assertEquals("0.25", result.getOrNull())
    }

    @Test
    fun `trailing zeros are stripped`() {
        val result = engine.evaluate("1.0+2.0")
        assertEquals("3", result.getOrNull())
    }

    @Test
    fun `long decimal is capped at 10 places`() {
        val result = engine.evaluate("1/3")
        assertTrue(result.isSuccess)
        val value = result.getOrThrow()
        // 1/3 = 0.333... should be capped
        val decimalPlaces = value.substringAfter('.').length
        assertTrue(decimalPlaces <= 10)
    }

    @Test
    fun `exact short decimal is not capped`() {
        val result = engine.evaluate("1/8")
        assertEquals("0.125", result.getOrNull())
    }

    @Test
    fun `large integer result`() {
        val result = engine.evaluate("999999\u00D7999999")
        assertTrue(result.isSuccess)
        assertEquals("999998000001", result.getOrNull())
    }

    @Test
    fun `zero result`() {
        val result = engine.evaluate("5\u22125")
        assertTrue(result.isSuccess)
        assertEquals("0", result.getOrNull())
    }

    @Test
    fun `negative result`() {
        val result = engine.evaluate("3\u22125")
        assertTrue(result.isSuccess)
        assertEquals("-2", result.getOrNull())
    }

    // ========== Error handling ==========

    @Test
    fun `empty string returns failure`() {
        val result = engine.evaluate("")
        assertTrue(result.isFailure)
    }

    @Test
    fun `blank string returns failure`() {
        val result = engine.evaluate("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun `division by zero returns failure`() {
        val result = engine.evaluate("1\u00F70")
        assertTrue(result.isFailure)
    }

    @Test
    fun `invalid expression returns failure`() {
        val result = engine.evaluate("++")
        assertTrue(result.isFailure)
    }

    @Test
    fun `factorial of negative returns failure`() {
        val result = engine.evaluate("(-1)!")
        assertTrue(result.isFailure)
    }

    @Test
    fun `sqrt of negative returns failure`() {
        val result = engine.evaluate("\u221A((-1))")
        assertTrue(result.isFailure)
    }

    @Test
    fun `factorial too large returns failure`() {
        val result = engine.evaluate("171!")
        assertTrue(result.isFailure)
    }

    // ========== Complex expressions through the full engine pipeline ==========

    @Test
    fun `order of operations`() {
        val result = engine.evaluate("2+3\u00D74")
        assertEquals("14", result.getOrNull())
    }

    @Test
    fun `parentheses override precedence`() {
        val result = engine.evaluate("(2+3)\u00D74")
        assertEquals("20", result.getOrNull())
    }

    @Test
    fun `nested parentheses`() {
        val result = engine.evaluate("((2+3))\u00D7((4))")
        assertEquals("20", result.getOrNull())
    }

    @Test
    fun `unary minus`() {
        val result = engine.evaluate("-5+3")
        assertTrue(result.isSuccess)
        assertEquals("-2", result.getOrNull())
    }

    @Test
    fun `percent in expression`() {
        val result = engine.evaluate("50%")
        assertEquals("0.5", result.getOrNull())
    }

    @Test
    fun `factorial in expression`() {
        val result = engine.evaluate("5!")
        assertEquals("120", result.getOrNull())
    }

    @Test
    fun `sqrt in expression`() {
        val result = engine.evaluate("\u221A(9)")
        assertEquals("3", result.getOrNull())
    }

    @Test
    fun `pi constant`() {
        val result = engine.evaluate("\u03C0")
        assertTrue(result.isSuccess)
        val value = result.getOrThrow().toDouble()
        assertTrue(value > 3.14)
        assertTrue(value < 3.15)
    }

    @Test
    fun `e constant`() {
        val result = engine.evaluate("e")
        assertTrue(result.isSuccess)
        val value = result.getOrThrow().toDouble()
        assertTrue(value > 2.71)
        assertTrue(value < 2.72)
    }

    @Test
    fun `implicit multiplication with parentheses`() {
        val result = engine.evaluate("2(3)")
        assertEquals("6", result.getOrNull())
    }

    @Test
    fun `implicit multiplication with constant`() {
        val result = engine.evaluate("2\u03C0")
        assertTrue(result.isSuccess)
        val value = result.getOrThrow().toDouble()
        assertTrue(value > 6.28)
        assertTrue(value < 6.29)
    }

    // ========== Mixed operations ==========

    @Test
    fun `addition and percent`() {
        val result = engine.evaluate("100+50%")
        assertEquals("150", result.getOrNull())
    }

    @Test
    fun `subtraction and percent`() {
        val result = engine.evaluate("100-20%")
        assertEquals("80", result.getOrNull())
    }

    @Test
    fun `double unary minus`() {
        assertEquals("5", engine.evaluate("--5").getOrNull())
    }

    @Test
    fun `unknown character returns failure`() {
        assertTrue(engine.evaluate("1@2").isFailure)
    }

    @Test
    fun `high precision sqrt of two`() {
        val result = engine.evaluate("\u221A(2)")
        assertTrue(result.isSuccess)
        val v = result.getOrThrow()
        // sqrt(2) ≈ 1.41421356237…; engine formats at 10 decimal places → 1.4142135624
        assertEquals("1.4142135624", v)
    }

    @Test
    fun `factorial and addition`() {
        val result = engine.evaluate("3!+4!")
        assertEquals("30", result.getOrNull())
    }

    @Test
    fun `sqrt and multiplication`() {
        val result = engine.evaluate("\u221A(4)\u00D73")
        assertTrue(result.isSuccess)
        assertEquals("6", result.getOrNull())
    }

    @Test
    fun `complex mixed expression`() {
        // (3! + sqrt(16)) * 2 = (6 + 4) * 2 = 20
        val result = engine.evaluate("(3!+\u221A(16))\u00D72")
        assertEquals("20", result.getOrNull())
    }

    // ========== Single number expressions ==========

    @Test
    fun `single digit`() {
        assertEquals("5", engine.evaluate("5").getOrNull())
    }

    @Test
    fun `single large number`() {
        assertEquals("123456789", engine.evaluate("123456789").getOrNull())
    }

    @Test
    fun `single decimal`() {
        assertEquals("3.14", engine.evaluate("3.14").getOrNull())
    }

    @Test
    fun `single negative number`() {
        assertEquals("-7", engine.evaluate("-7").getOrNull())
    }

    // ========== Auto-closed parentheses (as the ViewModel would send) ==========

    @Test
    fun `expression with auto-closed parentheses`() {
        val result = engine.evaluate("(2+3)")
        assertEquals("5", result.getOrNull())
    }

    @Test
    fun `sqrt with auto-closed parentheses`() {
        val result = engine.evaluate("\u221A(25)")
        assertEquals("5", result.getOrNull())
    }

    // ========== Edge: very small and very large values ==========

    @Test
    fun `very small decimal`() {
        val result = engine.evaluate("1/1000000")
        assertTrue(result.isSuccess)
        assertEquals("0.000001", result.getOrNull())
    }

    @Test
    fun `zero factorial`() {
        assertEquals("1", engine.evaluate("0!").getOrNull())
    }

    @Test
    fun `one factorial`() {
        assertEquals("1", engine.evaluate("1!").getOrNull())
    }

    // ========== Bug fix: double decimal rejection ==========

    @Test
    fun `double decimal in number returns failure`() {
        val result = engine.evaluate("5.5.5")
        assertTrue(result.isFailure)
    }

    @Test
    fun `leading decimal is still valid`() {
        val result = engine.evaluate(".5+1")
        assertTrue(result.isSuccess)
        assertEquals("1.5", result.getOrNull())
    }

    // ========== Bug fix: scientific notation for large numbers ==========

    @Test
    fun `very large factorial uses scientific notation`() {
        val result = engine.evaluate("170!")
        assertTrue(result.isSuccess)
        val value = result.getOrThrow()
        assertTrue("Large factorial should use scientific notation", value.contains("E+"))
        assertTrue("Scientific notation should be concise", value.length < 20)
    }

    @Test
    fun `moderately large number does not use scientific notation`() {
        val result = engine.evaluate("999999×999999")
        assertTrue(result.isSuccess)
        assertEquals("999998000001", result.getOrNull())
    }

    @Test
    fun `15 digit number does not use scientific notation`() {
        val result = engine.evaluate("100000000000000+1")
        assertTrue(result.isSuccess)
        assertEquals("100000000000001", result.getOrNull())
    }

    // ========== Integer formatting edge cases ==========

    @Test
    fun `15 nines stays plain`() {
        val result = engine.evaluate("999999999999999")
        assertEquals("999999999999999", result.getOrNull())
    }

    @Test
    fun `16 digit integer switches to scientific`() {
        // 999999999999999 + 1 = 1E15 (16-digit integer) — exceeds MAX_INTEGER_DIGITS.
        val result = engine.evaluate("999999999999999+1")
        assertTrue(result.isSuccess)
        val value = result.getOrThrow()
        assertTrue("Expected scientific notation: $value", value.contains("E+"))
    }

    @Test
    fun `negative 16 digit integer switches to scientific`() {
        val result = engine.evaluate("-999999999999999-1")
        assertTrue(result.isSuccess)
        val value = result.getOrThrow()
        assertTrue("Expected scientific notation: $value", value.contains("E+"))
        assertTrue("Expected negative sign: $value", value.startsWith("-"))
    }

    @Test
    fun `negative zero displays as zero`() {
        val result = engine.evaluate("0\u00D7(-1)")
        assertEquals("0", result.getOrNull())
    }

    // ========== Decimal formatting edge cases ==========

    @Test
    fun `two thirds rounds HALF_UP to ten places`() {
        val result = engine.evaluate("2/3")
        assertEquals("0.6666666667", result.getOrNull())
    }

    @Test
    fun `one seventh capped at ten places`() {
        val result = engine.evaluate("1/7")
        val value = result.getOrThrow()
        val fractional = value.substringAfter('.')
        assertTrue(fractional.length <= 10)
        assertTrue(value.startsWith("0.142857"))
    }

    @Test
    fun `one over one thousand twenty four is exact ten places`() {
        assertEquals("0.0009765625", engine.evaluate("1/1024").getOrNull())
    }

    @Test
    fun `one sixteenth is exact short decimal`() {
        assertEquals("0.0625", engine.evaluate("1/16").getOrNull())
    }

    @Test
    fun `one thirty-second is exact short decimal`() {
        assertEquals("0.03125", engine.evaluate("1/32").getOrNull())
    }

    @Test
    fun `one billionth preserves scale`() {
        assertEquals("0.000000001", engine.evaluate("1/1000000000").getOrNull())
    }

    @Test
    fun `negative one quarter`() {
        assertEquals("-0.25", engine.evaluate("-1/4").getOrNull())
    }

    @Test
    fun `one eleventh rounds at ten places`() {
        val value = engine.evaluate("1/11").getOrThrow()
        assertTrue("value=$value", value.startsWith("0.0909"))
        val fractional = value.substringAfter('.')
        assertTrue(fractional.length <= 10)
    }

    // ========== Scientific notation for large factorials ==========

    @Test
    fun `169 factorial uses scientific notation`() {
        val result = engine.evaluate("169!")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().contains("E+"))
    }

    @Test
    fun `150 factorial uses scientific notation`() {
        val result = engine.evaluate("150!")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().contains("E+"))
    }

    // ========== Sequential evaluations ==========

    @Test
    fun `sequential evaluations are independent`() {
        assertEquals("3", engine.evaluate("1+2").getOrNull())
        assertEquals("5", engine.evaluate("2+3").getOrNull())
        assertEquals("7", engine.evaluate("3+4").getOrNull())
        assertTrue(engine.evaluate("1\u00F70").isFailure)
        assertEquals("9", engine.evaluate("4+5").getOrNull())
    }

    // ========== Mixed ASCII and unicode operators ==========

    @Test
    fun `mixed ASCII and unicode operators`() {
        val result = engine.evaluate("2\u00D73+4\u22121\u00F72")
        // 2*3 + 4 - 1/2 = 6 + 4 - 0.5 = 9.5
        assertEquals("9.5", result.getOrNull())
    }

    // ========== Long expressions ==========

    @Test
    fun `50 term addition chain`() {
        val expr = (1..50).joinToString("+") { it.toString() }
        val result = engine.evaluate(expr)
        assertEquals("1275", result.getOrNull())
    }

    @Test
    fun `50 term multiplication chain of ones`() {
        val expr = (1..50).joinToString("\u00D7") { "1" }
        assertEquals("1", engine.evaluate(expr).getOrNull())
    }

    // ========== Error type pass-through ==========

    @Test
    fun `division by zero surfaces ArithmeticException`() {
        val result = engine.evaluate("1\u00F70")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ArithmeticException)
    }

    @Test
    fun `unknown character surfaces IllegalArgumentException`() {
        val result = engine.evaluate("1@2")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `factorial over max surfaces ArithmeticException`() {
        val result = engine.evaluate("171!")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ArithmeticException)
    }

    @Test
    fun `unmatched left paren surfaces IllegalArgumentException`() {
        val result = engine.evaluate("(1+2")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `sqrt of negative surfaces ArithmeticException`() {
        val result = engine.evaluate("\u221A(-4)")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ArithmeticException)
    }
}
