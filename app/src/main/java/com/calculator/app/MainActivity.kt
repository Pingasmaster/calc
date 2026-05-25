package com.calculator.app

import android.graphics.Color
import android.os.Bundle
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
        splash.setKeepOnScreenCondition { app.themeSettings.value == null }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
