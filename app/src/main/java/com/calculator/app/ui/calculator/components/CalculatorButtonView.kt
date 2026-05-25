package com.calculator.app.ui.calculator.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calculator.app.R
import com.calculator.app.domain.model.ButtonCategory
import com.calculator.app.domain.model.CalculatorButton
import com.calculator.app.ui.theme.CalculatorShapes
import com.calculator.app.ui.theme.buttonLarge
import com.calculator.app.ui.theme.buttonMedium
import com.calculator.app.ui.theme.buttonSmall

data class ButtonColors(val containerColor: Color, val contentColor: Color)

@Composable
fun rememberButtonColors(category: ButtonCategory): ButtonColors {
    val colorScheme = MaterialTheme.colorScheme
    return remember(category, colorScheme) {
        when (category) {
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
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalculatorButtonView(
    button: CalculatorButton,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    hapticsEnabled: Boolean = true,
) {
    val colors = rememberButtonColors(button.category)
    val haptic = LocalHapticFeedback.current
    val hapticType = when (button.category) {
        ButtonCategory.EQUALS -> HapticFeedbackType.Confirm
        ButtonCategory.AC -> HapticFeedbackType.Reject
        else -> HapticFeedbackType.KeyboardTap
    }
    // Memoize so the Button doesn't see a brand-new lambda identity on every
    // recomposition — keeps the inner Material Button skippable.
    val onClickWithHaptic = remember(haptic, hapticType, onClick, hapticsEnabled) {
        {
            if (hapticsEnabled) haptic.performHapticFeedback(hapticType)
            onClick()
        }
    }
    // The user already felt the initial click pulse; a second LongPress pulse
    // is just extra vibrator runtime. Long-press fires its callback silently.
    val onLongClickHandler: (() -> Unit)? = remember(onLongClick) {
        if (onLongClick != null) {
            { onLongClick() }
        } else {
            null
        }
    }

    when (button.category) {
        ButtonCategory.SCIENTIFIC -> {
            TextButton(
                onClick = onClickWithHaptic,
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
            Box(
                modifier = modifier
                    .clip(CalculatorShapes.BackspaceButton)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onClickWithHaptic,
                        onLongClick = onLongClickHandler,
                    )
                    .semantics { contentDescription = button.contentDescription },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_backspace),
                    contentDescription = button.contentDescription,
                    tint = colors.contentColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        ButtonCategory.EQUALS -> {
            Button(
                onClick = onClickWithHaptic,
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
                onClick = onClickWithHaptic,
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
                onClick = onClickWithHaptic,
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
                onClick = onClickWithHaptic,
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
                onClick = onClickWithHaptic,
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
