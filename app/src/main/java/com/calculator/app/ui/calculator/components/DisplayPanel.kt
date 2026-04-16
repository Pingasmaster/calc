package com.calculator.app.ui.calculator.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.calculator.app.R
import com.calculator.app.domain.model.CalculatorState

private const val TEXT_LENGTH_SMALL_THRESHOLD = 12
private const val TEXT_LENGTH_MEDIUM_THRESHOLD = 8

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DisplayPanel(
    state: CalculatorState,
    expressionField: TextFieldState,
    modifier: Modifier = Modifier,
) {
    val isEditing = !state.isResultDisplayed && !state.isError
    val expressionText = if (state.isResultDisplayed) state.expression else ""
    val previewText = state.previewResult
    val mainText = if (isEditing) expressionField.text.toString() else state.displayText

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        val motionScheme = MaterialTheme.motionScheme

        // Expression line (shown above result when = was pressed)
        AnimatedContent(
            targetState = expressionText,
            transitionSpec = {
                (slideInVertically(motionScheme.defaultSpatialSpec()) { -it } + fadeIn(motionScheme.defaultEffectsSpec()))
                    .togetherWith(
                        slideOutVertically(motionScheme.defaultSpatialSpec()) { it } + fadeOut(motionScheme.defaultEffectsSpec())
                    )
            },
            label = "expression",
        ) { expression ->
            if (expression.isNotEmpty()) {
                Text(
                    text = expression,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Main display
        val displayColor = if (state.isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        val dynamicStyle = when {
            mainText.length > TEXT_LENGTH_SMALL_THRESHOLD -> MaterialTheme.typography.displaySmall
            mainText.length > TEXT_LENGTH_MEDIUM_THRESHOLD -> MaterialTheme.typography.displayMedium
            else -> MaterialTheme.typography.displayLarge
        }

        if (isEditing) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            val cursorColor = MaterialTheme.colorScheme.primary
            val cursorBrush = remember(cursorColor) { SolidColor(cursorColor) }

            BasicTextField(
                state = expressionField,
                readOnly = true,
                textStyle = dynamicStyle.copy(
                    textAlign = TextAlign.End,
                    color = displayColor,
                ),
                lineLimits = TextFieldLineLimits.SingleLine,
                cursorBrush = cursorBrush,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                decorator = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        if (expressionField.text.isEmpty()) {
                            Text(
                                text = "0",
                                style = dynamicStyle,
                                color = displayColor.copy(alpha = 0.38f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        } else {
            // Result/Error mode: static text, no cursor
            Text(
                text = mainText,
                style = dynamicStyle,
                color = displayColor,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Live preview result (shown when typing, not after =)
        AnimatedContent(
            targetState = if (isEditing && previewText.isNotEmpty() && previewText != mainText) {
                previewText
            } else {
                ""
            },
            transitionSpec = {
                fadeIn(motionScheme.fastEffectsSpec()) togetherWith fadeOut(motionScheme.fastEffectsSpec())
            },
            label = "preview",
        ) { preview ->
            if (preview.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.display_preview_prefix, preview),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }
    }
}
