package com.calculator.app.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline Profile collector for the Calculator app.
 *
 * Walks the four Critical User Journeys (CUJs) listed in the per-app config:
 *  - cold_start_mainactivity
 *  - enter_arithmetic_expression
 *  - scroll_history_drawer
 *  - toggle_scientific_mode
 *
 * The output is written by `BaselineProfileRule` to the release variant's
 * generated-baseline-profiles directory, which `app/build.gradle.kts` then
 * packages into `baseline-prof.txt` + `startup-prof.txt`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        rule.collect(
            packageName = "com.calculator.app",
            stableIterations = 3,
            maxIterations = 8,
            includeInStartupProfile = true,
        ) {
            // ---- CUJ 1: cold start MainActivity ----
            // pressHome() ensures we kill the warm path; the next start is a true cold.
            pressHome()
            startActivityAndWait()

            // Wait for the button grid to actually be rendered (rather than just
            // the splash) before exercising the calculator so we don't
            // accidentally include splash-screen fade-out frames in the profile.
            device.wait(Until.hasObject(By.desc("All clear")), 5_000)

            // ---- CUJ 2: enter an arithmetic expression ----
            // 12 + 34 =
            device.findObject(By.desc("1")).click()
            device.findObject(By.desc("2")).click()
            device.findObject(By.desc("Add")).click()
            device.findObject(By.desc("3")).click()
            device.findObject(By.desc("4")).click()
            device.findObject(By.desc("Equals")).click()
            device.waitForIdle()

            // And a decimal: .5 × 6 =
            device.findObject(By.desc("All clear")).click()
            device.findObject(By.desc("Decimal point")).click()
            device.findObject(By.desc("5")).click()
            device.findObject(By.desc("Multiply")).click()
            device.findObject(By.desc("6")).click()
            device.findObject(By.desc("Equals")).click()
            device.waitForIdle()

            // ---- CUJ 3: scroll the history drawer ----
            // The history surface is reached by drag-down on the display in
            // compact widths; in expanded widths it is always visible. We try
            // a swipe first (covers compact, harmless if history is the side
            // panel because the swipe falls inside the screen bounds), then
            // scroll whatever scrollable surface is now on screen.
            val displayWidth = device.displayWidth
            device.swipe(displayWidth / 2, 200, displayWidth / 2, 1200, 30)
            device.findObject(By.desc("History"))
                ?: device.wait(Until.hasObject(By.desc("History")), 3_000)
            // Fling the history LazyColumn a few times.
            repeat(3) {
                device.swipe(
                    /* startX = */ 500,
                    /* startY = */ 1500,
                    /* endX = */ 500,
                    /* endY = */ 400,
                    /* steps = */ 20,
                )
                device.waitForIdle()
            }
            // Dismiss the history sheet.
            device.pressBack()
            device.waitForIdle()

            // ---- CUJ 4: touch the scientific row ----
            // The scientific row is permanently visible at the top of the
            // button area in this app (no actual toggle switch). Exercising
            // it warms the SCENTIFIC button-code path on a real user path.
            device.findObject(By.desc("Square root")).click()
            device.findObject(By.desc("Pi")).click()
            device.findObject(By.desc("Euler's number")).click()
            device.findObject(By.desc("Factorial")).click()
            device.findObject(By.desc("Backspace")).click()
            device.findObject(By.desc("All clear")).click()
            device.waitForIdle()
        }
    }
}