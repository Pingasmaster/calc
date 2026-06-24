package com.calculator.app.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scroll benchmarks for the Calculator app's history surface.
 *
 * Uses FrameTimingMetric to capture per-frame jank while swiping the history
 * LazyColumn. We open the history drawer (swipe down on the display in
 * compact layouts, always visible in expanded) and then fling a few times.
 *
 * Compares the same "None vs Partial(baselineProfileMode=Require)" pair as
 * StartupBenchmarks so CI surfaces a full speedup story.
 */
@RunWith(AndroidJUnit4::class)
class ScrollBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun scrollNone() {
        rule.measureRepeated(
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.None(),
            iterations = 5,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                device.wait(
                    Until.hasObject(By.desc("All clear")),
                    5_000,
                )
            },
        ) {
            // Open history (drag down on display in compact layout).
            val width = device.displayWidth / 2
            device.swipe(width, 200, width, 1200, 30)
            device.wait(Until.hasObject(By.desc("History")), 3_000)

            // Fling the list a few times so we get a sustained, jank-able
            // window for the metric.
            repeat(3) {
                device.swipe(width, 1500, width, 400, 20)
                device.waitForIdle()
            }
            device.pressBack()
        }
    }

    @Test
    fun scrollBaselineProfile() {
        rule.measureRepeated(
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(
                baselineProfileMode = BaselineProfileMode.Require,
            ),
            iterations = 5,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                device.wait(
                    Until.hasObject(By.desc("All clear")),
                    5_000,
                )
            },
        ) {
            val width = device.displayWidth / 2
            device.swipe(width, 200, width, 1200, 30)
            device.wait(Until.hasObject(By.desc("History")), 3_000)

            repeat(3) {
                device.swipe(width, 1500, width, 400, 20)
                device.waitForIdle()
            }
            device.pressBack()
        }
    }
}