package com.calculator.app.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.calculator.app.CalculatorApplication
import com.calculator.app.data.local.preferences.ThemeMode
import com.calculator.app.ui.calculator.CalculatorScreen
import com.calculator.app.ui.calculator.CalculatorViewModel
import com.calculator.app.ui.history.HistoryBottomSheet
import com.calculator.app.ui.history.HistoryPanel
import com.calculator.app.ui.settings.SettingsSheet
import kotlinx.coroutines.launch

@Composable
fun AdaptiveCalculatorLayout(
    windowAdaptiveInfo: WindowAdaptiveInfo,
) {
    val viewModel: CalculatorViewModel = viewModel()
    val historyItems by viewModel.history.collectAsState()
    val windowSizeClass = windowAdaptiveInfo.windowSizeClass

    val context = LocalContext.current
    val prefs = (context.applicationContext as CalculatorApplication).userPreferences
    val themeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val dynamicColor by prefs.dynamicColorEnabled.collectAsState(initial = true)
    val oledBlack by prefs.oledBlackEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .systemBarsPadding(),
            ) {
                HistoryPanel(
                    historyItems = historyItems,
                    onItemClick = { viewModel.loadFromHistory(it) },
                    onClearAll = { viewModel.clearHistory() },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                VerticalDivider()

                CalculatorScreen(
                    viewModel = viewModel,
                    onSettingsClick = { showSettings = true },
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                )
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            ) {
                CalculatorScreen(
                    viewModel = viewModel,
                    onDisplayClick = { showHistory = true },
                    onSettingsClick = { showSettings = true },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (showHistory) {
                HistoryBottomSheet(
                    historyItems = historyItems,
                    onDismiss = { showHistory = false },
                    onItemClick = { viewModel.loadFromHistory(it) },
                    onClearAll = { viewModel.clearHistory() },
                )
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            themeMode = themeMode,
            dynamicColor = dynamicColor,
            oledBlack = oledBlack,
            onThemeModeChange = { scope.launch { prefs.setThemeMode(it) } },
            onDynamicColorChange = { scope.launch { prefs.setDynamicColor(it) } },
            onOledBlackChange = { scope.launch { prefs.setOledBlack(it) } },
            onDismiss = { showSettings = false },
        )
    }
}
