package com.calculator.app.ui.calculator.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calculator.app.domain.model.ButtonCategory
import com.calculator.app.domain.model.CalculatorButton
import com.calculator.app.ui.theme.CalculatorShapes
import com.calculator.app.ui.theme.buttonLarge
import com.calculator.app.ui.theme.buttonMedium
import com.calculator.app.ui.theme.buttonSmall

data class ButtonColors(
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
fun rememberButtonColors(category: ButtonCategory): ButtonColors {
    val colorScheme = MaterialTheme.colorScheme
    return when (category) {
        ButtonCategory.NUMBER -> ButtonColors(
            containerColor = colorScheme.surfaceContainerHigh,
            contentColor = colorScheme.onSurface,
        )
        ButtonCategory.OPERATOR -> ButtonColors(
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer,
        )
        ButtonCategory.FUNCTION -> ButtonColors(
            containerColor = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer,
        )
        ButtonCategory.AC -> ButtonColors(
            containerColor = colorScheme.surfaceContainerHighest,
            contentColor = colorScheme.onSurface,
        )
        ButtonCategory.EQUALS -> ButtonColors(
            containerColor = colorScheme.primary,
            contentColor = colorScheme.onPrimary,
        )
        ButtonCategory.SCIENTIFIC -> ButtonColors(
            containerColor = Color.Transparent,
            contentColor = colorScheme.onSurfaceVariant,
        )
        ButtonCategory.BACKSPACE -> ButtonColors(
            containerColor = Color.Transparent,
            contentColor = colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalculatorButtonView(
    button: CalculatorButton,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = rememberButtonColors(button.category)

    when (button.category) {
        ButtonCategory.SCIENTIFIC -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.semantics { contentDescription = button.contentDescription },
                shape = CalculatorShapes.Button,
                interactionSource = interactionSource,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = button.symbol,
                    style = MaterialTheme.typography.buttonSmall,
                    color = colors.contentColor,
                )
            }
        }

        ButtonCategory.BACKSPACE -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.semantics { contentDescription = button.contentDescription },
                shapes = ButtonDefaults.shapes(
                    shape = CalculatorShapes.BackspaceButton,
                    pressedShape = CalculatorShapes.BackspaceButtonPressed,
                ),
                interactionSource = interactionSource,
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = button.contentDescription,
                    tint = colors.contentColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        ButtonCategory.EQUALS -> {
            Button(
                onClick = onClick,
                modifier = modifier.semantics { contentDescription = button.contentDescription },
                shapes = ButtonDefaults.shapes(
                    shape = CalculatorShapes.WideButton,
                    pressedShape = CalculatorShapes.WideButtonPressed,
                ),
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.containerColor,
                    contentColor = colors.contentColor,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = button.symbol,
                    style = MaterialTheme.typography.buttonLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }

        ButtonCategory.AC -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier.semantics { contentDescription = button.contentDescription },
                shapes = ButtonDefaults.shapes(
                    shape = CalculatorShapes.Button,
                    pressedShape = CalculatorShapes.ButtonPressed,
                ),
                interactionSource = interactionSource,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.containerColor,
                    contentColor = colors.contentColor,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = button.symbol,
                    style = MaterialTheme.typography.buttonMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        ButtonCategory.NUMBER -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier.semantics { contentDescription = button.contentDescription },
                shapes = ButtonDefaults.shapes(
                    shape = CalculatorShapes.Button,
                    pressedShape = CalculatorShapes.ButtonPressed,
                ),
                interactionSource = interactionSource,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.containerColor,
                    contentColor = colors.contentColor,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = button.symbol,
                    style = MaterialTheme.typography.buttonLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }

        ButtonCategory.OPERATOR -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier.semantics { contentDescription = button.contentDescription },
                shapes = ButtonDefaults.shapes(
                    shape = CalculatorShapes.OperatorButton,
                    pressedShape = CalculatorShapes.OperatorButtonPressed,
                ),
                interactionSource = interactionSource,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.containerColor,
                    contentColor = colors.contentColor,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = button.symbol,
                    style = MaterialTheme.typography.buttonMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        ButtonCategory.FUNCTION -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier.semantics { contentDescription = button.contentDescription },
                shapes = ButtonDefaults.shapes(
                    shape = CalculatorShapes.Button,
                    pressedShape = CalculatorShapes.ButtonPressed,
                ),
                interactionSource = interactionSource,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.containerColor,
                    contentColor = colors.contentColor,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = button.symbol,
                    style = MaterialTheme.typography.buttonMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
