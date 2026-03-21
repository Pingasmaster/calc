package com.calculator.app.ui.calculator.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calculator.app.domain.model.ButtonCategory
import com.calculator.app.domain.model.CalculatorButton
import com.calculator.app.ui.theme.*

data class ButtonColors(
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
fun rememberButtonColors(category: ButtonCategory): ButtonColors {
    val isDark = isSystemInDarkTheme()
    return when (category) {
        ButtonCategory.NUMBER -> ButtonColors(
            containerColor = if (isDark) NumberButtonColorDark else NumberButtonColor,
            contentColor = if (isDark) NumberButtonContentColorDark else NumberButtonContentColor,
        )
        ButtonCategory.OPERATOR -> ButtonColors(
            containerColor = if (isDark) OperatorButtonColorDark else OperatorButtonColor,
            contentColor = if (isDark) OperatorButtonContentColorDark else OperatorButtonContentColor,
        )
        ButtonCategory.FUNCTION -> ButtonColors(
            containerColor = if (isDark) FunctionButtonColorDark else FunctionButtonColor,
            contentColor = if (isDark) FunctionButtonContentColorDark else FunctionButtonContentColor,
        )
        ButtonCategory.AC -> ButtonColors(
            containerColor = if (isDark) ACButtonColorDark else ACButtonColor,
            contentColor = if (isDark) ACButtonContentColorDark else ACButtonContentColor,
        )
        ButtonCategory.EQUALS -> ButtonColors(
            containerColor = if (isDark) EqualsButtonColorDark else EqualsButtonColor,
            contentColor = if (isDark) EqualsButtonContentColorDark else EqualsButtonContentColor,
        )
        ButtonCategory.SCIENTIFIC -> ButtonColors(
            containerColor = Color.Transparent,
            contentColor = if (isDark) ScientificTextColorDark else ScientificTextColor,
        )
        ButtonCategory.BACKSPACE -> ButtonColors(
            containerColor = Color.Transparent,
            contentColor = if (isDark) BackspaceIconColorDark else BackspaceIconColor,
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
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
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
                    modifier = Modifier.size(22.dp),
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
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
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
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
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
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
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
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
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
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
