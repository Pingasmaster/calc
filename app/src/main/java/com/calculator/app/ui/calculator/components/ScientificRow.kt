@file:Suppress("DEPRECATION") // Newer ButtonGroup overloads use non-composable content builder; migration pending API stabilization

package com.calculator.app.ui.calculator.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calculator.app.domain.model.ButtonCategory
import com.calculator.app.domain.model.scientificRow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScientificRow(
    onButtonClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLongPressBackspace: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val availableWidth = maxWidth
        val spacing = 8.dp
        // Match ButtonGrid's function-row height so both sections scale consistently
        val totalWeight = 3f + 0.75f
        val totalSpacing = spacing * 3
        val numberButtonWidth = (availableWidth - totalSpacing) * (1f / totalWeight)
        val rowHeight = numberButtonWidth * 0.83f

        ButtonGroup(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            scientificRow.forEach { btn ->
                val interactionSource = remember { MutableInteractionSource() }
                CalculatorButtonView(
                    button = btn,
                    onClick = { onButtonClick(btn.symbol) },
                    modifier = Modifier
                        .weight(1f)
                        .animateWidth(interactionSource),
                    onLongClick = if (btn.category == ButtonCategory.BACKSPACE) onLongPressBackspace else null,
                    interactionSource = interactionSource,
                )
            }
        }
    }
}
