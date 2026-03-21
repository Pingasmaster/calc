package com.calculator.app.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.calculator.app.ui.calculator.CalculatorScreen
import com.calculator.app.ui.calculator.CalculatorViewModel
import com.calculator.app.ui.history.HistoryBottomSheet
import com.calculator.app.ui.history.HistoryPanel

@Composable
fun AdaptiveCalculatorLayout(
    windowAdaptiveInfo: WindowAdaptiveInfo,
) {
    val viewModel: CalculatorViewModel = viewModel()
    val historyItems by viewModel.history.collectAsState()
    val windowSizeClass = windowAdaptiveInfo.windowSizeClass

    when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
            ExpandedLayout(
                viewModel = viewModel,
                historyItems = historyItems,
            )
        }
        else -> {
            CompactLayout(
                viewModel = viewModel,
                historyItems = historyItems,
            )
        }
    }
}

@Composable
private fun CompactLayout(
    viewModel: CalculatorViewModel,
    historyItems: List<com.calculator.app.domain.model.HistoryItem>,
) {
    var showHistory by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        CalculatorScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize(),
        )

        // History button in top-right corner
        IconButton(
            onClick = { showHistory = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp, top = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "History",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    // Native M3 ModalBottomSheet for history
    if (showHistory) {
        HistoryBottomSheet(
            historyItems = historyItems,
            onDismiss = { showHistory = false },
            onExpressionClick = { viewModel.loadFromHistory(it) },
            onResultClick = { viewModel.loadFromHistory(it) },
            onClearAll = { viewModel.clearHistory() },
        )
    }
}

@Composable
private fun ExpandedLayout(
    viewModel: CalculatorViewModel,
    historyItems: List<com.calculator.app.domain.model.HistoryItem>,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding(),
    ) {
        // History panel on the left (1/3)
        HistoryPanel(
            historyItems = historyItems,
            onExpressionClick = { viewModel.loadFromHistory(it) },
            onResultClick = { viewModel.loadFromHistory(it) },
            onClearAll = { viewModel.clearHistory() },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )

        VerticalDivider()

        // Calculator on the right (2/3)
        CalculatorScreen(
            viewModel = viewModel,
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight(),
        )
    }
}
