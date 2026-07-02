package com.calculator.app

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calculator.app.data.local.preferences.ThemeMode
import com.calculator.app.data.local.preferences.ThemeSettings
import com.calculator.app.ui.adaptive.AdaptiveCalculatorLayout
import com.calculator.app.ui.calculator.CalculatorViewModel
import com.calculator.app.ui.settings.SettingsViewModel
import com.calculator.app.ui.theme.CalculatorTheme

class MainActivity : ComponentActivity() {

    // ViewModels are owned by the Activity (not by any composable), which is
    // the only call site slack-compose-lints'
    // `ComposeViewModelInjection` will permit. `viewModels()` here scopes them
    // to the Activity lifecycle and gives them the same SavedStateHandle
    // wiring they had before the hoist.
    private val calculatorViewModel: CalculatorViewModel by viewModels(
        factoryProducer = { CalculatorViewModel.Factory },
    )
    private val settingsViewModel: SettingsViewModel by viewModels(
        factoryProducer = { SettingsViewModel.Factory },
    )

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

        // Calculator UI is static 99% of the time. Tell the system we'd rather
        // not have the display refresh at 90/120 Hz when nothing is moving —
        // it still bumps the rate during touch/animation. minSdk=35 means this
        // API (added in API 35) is always available.
        window.decorView.requestedFrameRate = View.REQUESTED_FRAME_RATE_CATEGORY_NORMAL

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
            DisposableEffect(barStyle) {
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }

            // Collect each ViewModel's state as Compose state right here at
            // the Activity-setContent scope — not inside AdaptiveCalculatorLayout.
            // This is the hoist point that satisfies both slack-compose-lints
            // rules: no `viewModel()` in a composable, no ViewModel forwarded
            // as a parameter.
            val historyItems by calculatorViewModel.history.collectAsStateWithLifecycle()
            val calcState by calculatorViewModel.state.collectAsStateWithLifecycle()

            CalculatorTheme(
                darkTheme = isDark,
                dynamicColor = s.dynamicColor,
                oledBlack = s.oledBlack,
            ) {
                val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
                AdaptiveCalculatorLayout(
                    windowSizeClass = windowAdaptiveInfo.windowSizeClass,
                    themeSettings = s,
                    historyItems = historyItems,
                    calcState = calcState,
                    expressionField = calculatorViewModel.expressionField,
                    onButtonClick = calculatorViewModel::onButtonClick,
                    onLoadFromHistory = calculatorViewModel::loadFromHistory,
                    onClearHistory = calculatorViewModel::clearHistory,
                    onFlushSaveState = calculatorViewModel::flushSaveStateNow,
                    onSetThemeMode = settingsViewModel::setThemeMode,
                    onSetDynamicColor = settingsViewModel::setDynamicColor,
                    onSetOledBlack = settingsViewModel::setOledBlack,
                    onHapticsChange = settingsViewModel::setHapticsEnabled,
                )
            }
        }
    }
}
