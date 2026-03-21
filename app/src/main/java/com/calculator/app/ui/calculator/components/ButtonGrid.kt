@file:Suppress("DEPRECATION")

package com.calculator.app.ui.calculator.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.calculator.app.domain.model.buttonRows

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ButtonGrid(
    onButtonClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val availableWidth = maxWidth
        val spacing = 8.dp
        // Number row: 3 buttons at weight 1f + 1 at 0.75f + 3 gaps = total weight 3.75
        // A single number button width ≈ (availableWidth - 3*spacing) * (1/3.75)
        // Height = button width for circles (1:1 aspect)
        val totalWeight = 3f + 0.75f
        val totalSpacing = spacing * 3
        val numberButtonWidth = (availableWidth - totalSpacing) * (1f / totalWeight)
        val rowHeight = numberButtonWidth  // square for number buttons
        val functionRowHeight = rowHeight * 0.83f  // function row is smaller

        Column(
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            buttonRows.forEachIndexed { rowIndex, row ->
                val isFirstRow = rowIndex == 0
                val isLastRow = rowIndex == buttonRows.lastIndex
                val currentRowHeight = when {
                    isFirstRow -> functionRowHeight
                    isLastRow -> rowHeight * 0.85f
                    else -> rowHeight
                }

                ButtonGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(currentRowHeight),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    row.forEach { btn ->
                        val interactionSource = remember { MutableInteractionSource() }
                        CalculatorButtonView(
                            button = btn,
                            onClick = { onButtonClick(btn.symbol) },
                            modifier = Modifier
                                .weight(btn.widthWeight)
                                .animateWidth(interactionSource),
                            interactionSource = interactionSource,
                        )
                    }
                }
            }
        }
    }
}
