package com.calculator.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calculator.app.data.local.preferences.ThemeMode
import com.calculator.app.data.local.preferences.ThemeSettings
import com.calculator.app.ui.adaptive.AdaptiveCalculatorLayout
import com.calculator.app.ui.theme.CalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as CalculatorApplication
        val splash = installSplashScreen()
        val splashStart = android.os.SystemClock.elapsedRealtime()
        // Defensive 2s ceiling so a degenerate slow-disk device can't hang us
        // forever waiting on the first DataStore emission.
        splash.setKeepOnScreenCondition {
            app.themeSettings.value == null &&
                android.os.SystemClock.elapsedRealtime() - splashStart < 2000L
        }

        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() is re-applied with theme-aware bar styles inside
        // the DisposableEffect below; calling it here too would just do the work twice.

        // Calculator UI is static 99% of the time. Tell Android 15+ that we'd
        // rather not have the display refresh at 90/120 Hz when nothing is
        // moving — the system still bumps the rate during touch/animation.
        if (Build.VERSION.SDK_INT >= 35) {
            @Suppress("NewApi")
            window.decorView.requestedFrameRate = View.REQUESTED_FRAME_RATE_CATEGORY_NORMAL
        }

        setContent {
            val settings by app.themeSettings.collectAsStateWithLifecycle()
            val s = settings ?: ThemeSettings.Default

            val isDark = when (s.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Re-apply edge-to-edge with proper light/dark icon contrast once we know the theme.
            val barStyle = remember(isDark) {
                if (isDark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
            }
            androidx.compose.runtime.DisposableEffect(barStyle) {
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }

            CalculatorTheme(
                darkTheme = isDark,
                dynamicColor = s.dynamicColor,
                oledBlack = s.oledBlack,
            ) {
                val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
                AdaptiveCalculatorLayout(
                    windowSizeClass = windowAdaptiveInfo.windowSizeClass,
                    themeSettings = s,
                )
            }
        }
    }
}
