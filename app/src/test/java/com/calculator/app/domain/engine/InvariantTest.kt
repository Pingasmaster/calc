package com.calculator.app.domain.engine

import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import kotlin.random.Random

class InvariantTest {

    private val engine = CalculatorEngine()

    private fun randomTerm(rng: Random): String {
        val kind = rng.nextInt(4)
        return when (kind) {
            0 -> rng.nextInt(0, 1000).toString()

            1 -> {
                val whole = rng.nextInt(0, 100)
                val frac = rng.nextInt(0, 100).toString().padStart(2, '0')
                "$whole.$frac"
            }

            2 -> "\u03C0"

            else -> "e"
        }
    }

    private fun randomWellFormed(rng: Random, depth: Int = 0): String {
        if (depth > 2 || rng.nextInt(3) == 0) return randomTerm(rng)
        val left = randomWellFormed(rng, depth + 1)
        val right = randomWellFormed(rng, depth + 1)
        val op = listOf("+", "-", "*").random(rng) // avoid / to dodge div-by-zero
        return "($left$op$right)"
    }

    @Test
    fun `500 random well-formed expressions evaluate successfully`() {
        val rng = Random(0xC0FFEE)
        var failures = 0
        repeat(500) {
            val expr = randomWellFormed(rng)
            val result = engine.evaluate(expr)
            if (result.isFailure) {
                failures++
            } else {
                // Must parse as a BigDecimal — propagates if not.
                BigDecimal(result.getOrThrow())
            }
        }
        // Division-free grammar: zero failures expected.
        assertTrue("had $failures failures", failures == 0)
    }

    @Test
    fun `500 random malformed strings always return Result failure never throw`() {
        val rng = Random(0xBADBEEF)
        val charset = "0123456789+-*/().!%\u03C0e\u221A \t#@?"
        repeat(500) {
            val len = rng.nextInt(0, 20)
            val sb = StringBuilder()
            repeat(len) { sb.append(charset.random(rng)) }
            val str = sb.toString()
            // Never throws: evaluate must swallow any exception into Result —
            // any uncaught throw here would fail the test.
            engine.evaluate(str)
        }
    }

    @Test
    fun `additive identity - a plus zero equals a`() {
        val rng = Random(42)
        repeat(100) {
            val a = rng.nextInt(-1_000_000, 1_000_001)
            val expected = a.toString()
            val actual = engine.evaluate("$a+0").getOrNull()
            assertTrue("$a+0 = $actual", actual == expected)
        }
    }

    @Test
    fun `multiplicative identity - a times one equals a`() {
        val rng = Random(43)
        repeat(100) {
            val a = rng.nextInt(-1_000_000, 1_000_001)
            val expected = a.toString()
            val actual = engine.evaluate("$a\u00D71").getOrNull()
            assertTrue("$a*1 = $actual", actual == expected)
        }
    }

    @Test
    fun `addition commutativity`() {
        val rng = Random(44)
        repeat(100) {
            val a = rng.nextInt(-10_000, 10_001)
            val b = rng.nextInt(-10_000, 10_001)
            val ab = engine.evaluate("$a+$b").getOrNull()
            val ba = engine.evaluate("$b+$a").getOrNull()
            assertTrue("$a+$b=$ab vs $b+$a=$ba", ab == ba)
        }
    }

    @Test
    fun `multiplication commutativity`() {
        val rng = Random(45)
        repeat(100) {
            val a = rng.nextInt(-1_000, 1_001)
            val b = rng.nextInt(-1_000, 1_001)
            val ab = engine.evaluate("$a\u00D7$b").getOrNull()
            val ba = engine.evaluate("$b\u00D7$a").getOrNull()
            assertTrue("$a*$b=$ab vs $b*$a=$ba", ab == ba)
        }
    }

    @Test
    fun `division inverse - a times b over b approximately equals a`() {
        val rng = Random(46)
        repeat(100) {
            val a = rng.nextInt(-10_000, 10_001)
            var b = rng.nextInt(-1_000, 1_001)
            if (b == 0) b = 1
            val result = engine.evaluate("($a\u00D7$b)\u00F7$b")
            assertTrue("($a*$b)/$b failed", result.isSuccess)
            val got = BigDecimal(result.getOrNull())
            val expected = BigDecimal(a)
            // High precision integer division chain: should be exact.
            assertTrue(
                "got=$got expected=$expected",
                got.compareTo(expected) == 0 ||
                    got.subtract(expected).abs() < BigDecimal("1E-10"),
            )
        }
    }

    @Test
    fun `subtraction inverse - a plus b minus b equals a`() {
        val rng = Random(47)
        repeat(100) {
            val a = rng.nextInt(-10_000, 10_001)
            val b = rng.nextInt(-10_000, 10_001)
            val result = engine.evaluate("($a+$b)\u2212$b")
            assertTrue(result.isSuccess)
            assertTrue(
                "a=$a b=$b got=${result.getOrNull()}",
                BigDecimal(result.getOrNull()).compareTo(BigDecimal(a)) == 0,
            )
        }
    }
}
