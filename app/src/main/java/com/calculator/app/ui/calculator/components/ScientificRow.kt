@file:Suppress("DEPRECATION")

package com.calculator.app.ui.calculator.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calculator.app.domain.model.scientificRow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScientificRow(
    onButtonClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ButtonGroup(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        scientificRow.forEach { btn ->
            val interactionSource = remember { MutableInteractionSource() }
            CalculatorButtonView(
                button = btn,
                onClick = { onButtonClick(btn.symbol) },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.4f)
                    .animateWidth(interactionSource),
                interactionSource = interactionSource,
            )
        }
    }
}
