package com.calculator.app.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.calculator.app.R
import com.calculator.app.ui.calculator.components.ButtonGrid
import com.calculator.app.ui.calculator.components.DisplayPanel
import com.calculator.app.ui.calculator.components.ScientificRow
import com.calculator.app.ui.theme.CalculatorShapes

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier,
    onDisplayClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Display area with rounded bottom corners — swipe down to open history
        Surface(
            shape = CalculatorShapes.HistoryOverlay,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(
                    if (onDisplayClick != null) {
                        Modifier.pointerInput(Unit) {
                            val dragThresholdPx = 80.dp.toPx()
                            var totalDrag = 0f
                            detectVerticalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                    if (totalDrag > dragThresholdPx) {
                                        onDisplayClick()
                                        totalDrag = 0f
                                    }
                                },
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                DisplayPanel(
                    state = state,
                    expressionField = viewModel.expressionField,
                    modifier = Modifier.fillMaxSize(),
                )

                // Settings icon in top-right
                if (onSettingsClick != null) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 8.dp, top = 4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings_icon),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Button area on lavender surface
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            ScientificRow(
                onButtonClick = viewModel::onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                onLongPressBackspace = { viewModel.onButtonClick("AC") },
            )

            ButtonGrid(
                onButtonClick = viewModel::onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }
    }
}
