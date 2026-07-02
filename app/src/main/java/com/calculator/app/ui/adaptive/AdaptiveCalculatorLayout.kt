package com.calculator.app.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.window.core.layout.WindowSizeClass
import com.calculator.app.data.local.preferences.ThemeMode
import com.calculator.app.data.local.preferences.ThemeSettings
import com.calculator.app.domain.model.CalculatorState
import com.calculator.app.domain.model.HistoryItem
import com.calculator.app.ui.calculator.CalculatorScreen
import com.calculator.app.ui.history.HistoryBottomSheet
import com.calculator.app.ui.history.HistoryPanel
import com.calculator.app.ui.settings.SettingsSheet
import kotlinx.collections.immutable.ImmutableList

/**
 * Stateful screen shell. ViewModels live one level up (in MainActivity) and
 * their state is hoisted in via plain params — children are stateless. This
 * is the canonical "state hoisting" pattern, and the only layout that
 * simultaneously satisfies both slack-compose-lints rules
 * (`ComposeViewModelForwarding` + `ComposeViewModelInjection`):
 *
 *   * `ComposeViewModelForwarding`: no ViewModel instance ever crosses a
 *     composable function boundary as a parameter.
 *   * `ComposeViewModelInjection`: `viewModel()` is only ever called from
 *     Activity/Fragment scope, never from a composable.
 */
@Composable
fun AdaptiveCalculatorLayout(
    windowSizeClass: WindowSizeClass,
    themeSettings: ThemeSettings,
    historyItems: ImmutableList<HistoryItem>,
    calcState: CalculatorState,
    expressionField: TextFieldState,
    onButtonClick: (String) -> Unit,
    onLoadFromHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onFlushSaveState: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetOledBlack: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // rememberUpdatedState pins a single reference for the duration of the
    // effect, so the `LifecycleEventEffect` below doesn't get re-keyed every
    // time the parent composable (MainActivity) recomposes and passes a fresh
    // lambda capture.
    val flushSaveState = rememberUpdatedState(onFlushSaveState)

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

    // Flush the debounced SavedStateHandle writes before the Activity hands the
    // outState Bundle to the system. ON_PAUSE fires before onSaveInstanceState
    // on Android 9+ (we're minSdk=33), so the flush lands in time.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        flushSaveState.value()
    }

    if (showSettings) {
        SettingsSheet(
            themeMode = themeSettings.themeMode,
            dynamicColor = themeSettings.dynamicColor,
            oledBlack = themeSettings.oledBlack,
            hapticsEnabled = themeSettings.hapticsEnabled,
            onThemeModeChange = onSetThemeMode,
            onDynamicColorChange = onSetDynamicColor,
            onOledBlackChange = onSetOledBlack,
            onHapticsEnabledChange = onHapticsChange,
            onDismiss = { showSettings = false },
        )
    }
}
