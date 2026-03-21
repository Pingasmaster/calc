package com.calculator.app.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calculator.app.ui.calculator.components.ButtonGrid
import com.calculator.app.ui.calculator.components.DisplayPanel
import com.calculator.app.ui.calculator.components.ScientificRow

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp),
    ) {
        // Display panel takes remaining space
        DisplayPanel(
            state = state,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        // Scientific function row
        ScientificRow(
            onButtonClick = viewModel::onButtonClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        )

        // Main button grid
        ButtonGrid(
            onButtonClick = viewModel::onButtonClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
    }
}
