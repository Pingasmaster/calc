package com.calculator.app.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Startup benchmarks for the Calculator app.
 *
 * Compares cold-start time-to-initial-display:
 *  - coldNone(): without a baseline profile (worst case, baseline mode = None)
 *  - coldBaselineProfile(): with the profile baked into the release variant
 *
 * The "None" iteration is what gives the relative-speedup number you see
 * in CI logs; "Partial / Require" with the baseline profile present is the
 * shipping, profile-enabled scenario.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldNone() {
        rule.measureRepeated(
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.COLD,
            iterations = 10,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
            // Wait for the button grid to be rendered so we're timing actual
            // time-to-initial-display, not splash fade-out.
            device.wait(
                Until.hasObject(By.desc("All clear")),
                5_000,
            )
        }
    }

    @Test
    fun coldBaselineProfile() {
        rule.measureRepeated(
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Require,
            ),
            startupMode = StartupMode.COLD,
            iterations = 10,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
            device.wait(
                Until.hasObject(By.desc("All clear")),
                5_000,
            )
        }
    }
}