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
        val value = result.getOrNull()!!
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
        val value = result.getOrNull()!!.toDouble()
        assertTrue(value > 3.14)
        assertTrue(value < 3.15)
    }

    @Test
    fun `e constant`() {
        val result = engine.evaluate("e")
        assertTrue(result.isSuccess)
        val value = result.getOrNull()!!.toDouble()
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
        val value = result.getOrNull()!!.toDouble()
        assertTrue(value > 6.28)
        assertTrue(value < 6.29)
    }

    // ========== Mixed operations ==========

    @Test
    fun `addition and percent`() {
        val result = engine.evaluate("100+50%")
        assertEquals("100.5", result.getOrNull())
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
}
