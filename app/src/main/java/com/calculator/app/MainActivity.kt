package com.calculator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.calculator.app.data.local.preferences.ThemeMode
import com.calculator.app.ui.adaptive.AdaptiveCalculatorLayout
import com.calculator.app.ui.theme.CalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = (application as CalculatorApplication).userPreferences

        setContent {
            val themeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColor by prefs.dynamicColorEnabled.collectAsState(initial = true)
            val oledBlack by prefs.oledBlackEnabled.collectAsState(initial = false)

            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CalculatorTheme(
                darkTheme = isDark,
                dynamicColor = dynamicColor,
                oledBlack = oledBlack,
            ) {
                val windowAdaptiveInfo = currentWindowAdaptiveInfo()
                AdaptiveCalculatorLayout(windowAdaptiveInfo = windowAdaptiveInfo)
            }
        }
    }
}
