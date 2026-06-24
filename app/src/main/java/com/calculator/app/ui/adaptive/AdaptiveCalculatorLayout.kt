package com.calculator.app.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.calculator.app.data.local.preferences.ThemeSettings
import com.calculator.app.ui.calculator.CalculatorScreen
import com.calculator.app.ui.calculator.CalculatorViewModel
import com.calculator.app.ui.history.HistoryBottomSheet
import com.calculator.app.ui.history.HistoryPanel
import com.calculator.app.ui.settings.SettingsSheet
import com.calculator.app.ui.settings.SettingsViewModel

@Composable
fun AdaptiveCalculatorLayout(
    windowSizeClass: WindowSizeClass,
    themeSettings: ThemeSettings,
    modifier: Modifier = Modifier,
    viewModel: CalculatorViewModel = viewModel(factory = CalculatorViewModel.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val historyItems by viewModel.history.collectAsStateWithLifecycle()
    val calcState by viewModel.state.collectAsStateWithLifecycle()
    val onButtonClick = remember(viewModel) { viewModel::onButtonClick }
    val onLoadFromHistory = remember(viewModel) { viewModel::loadFromHistory }
    val onClearHistory = remember(viewModel) { viewModel::clearHistory }
    val expressionField = viewModel.expressionField

    // Flush the debounced SavedStateHandle writes before the Activity hands the
    // outState Bundle to the system. ON_PAUSE fires before onSaveInstanceState
    // on Android 9+ (we're minSdk=33), so the flush lands in time.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.flushSaveStateNow()
    }

    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .safeDrawingPadding(),
            ) {
                HistoryPanel(
                    historyItems = historyItems,
                    onItemClick = onLoadFromHistory,
                    onClearAll = onClearHistory,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                VerticalDivider()

                CalculatorScreen(
                    state = calcState,
                    expressionField = expressionField,
                    onButtonClick = onButtonClick,
                    onSettingsClick = { showSettings = true },
                    hapticsEnabled = themeSettings.hapticsEnabled,
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                )
            }
        }

        else -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .safeDrawingPadding(),
            ) {
                CalculatorScreen(
                    state = calcState,
                    expressionField = expressionField,
                    onButtonClick = onButtonClick,
                    onDisplayClick = { showHistory = true },
                    onSettingsClick = { showSettings = true },
                    hapticsEnabled = themeSettings.hapticsEnabled,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (showHistory) {
                HistoryBottomSheet(
                    historyItems = historyItems,
                    onDismiss = { showHistory = false },
                    onItemClick = onLoadFromHistory,
                    onClearAll = onClearHistory,
                )
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            themeMode = themeSettings.themeMode,
            dynamicColor = themeSettings.dynamicColor,
            oledBlack = themeSettings.oledBlack,
            hapticsEnabled = themeSettings.hapticsEnabled,
            onThemeModeChange = settingsViewModel::setThemeMode,
            onDynamicColorChange = settingsViewModel::setDynamicColor,
            onOledBlackChange = settingsViewModel::setOledBlack,
            onHapticsEnabledChange = settingsViewModel::setHapticsEnabled,
            onDismiss = { showSettings = false },
        )
    }
}
