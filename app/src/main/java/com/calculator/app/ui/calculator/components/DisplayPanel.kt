package com.calculator.app.ui.calculator.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.calculator.app.domain.model.CalculatorState

private data class DisplayData(
    val mainText: String,
    val expressionText: String,
    val previewText: String,
    val isResult: Boolean,
    val isError: Boolean,
)

@Composable
fun DisplayPanel(
    state: CalculatorState,
    modifier: Modifier = Modifier,
) {
    val displayData = DisplayData(
        mainText = state.displayText,
        expressionText = if (state.isResultDisplayed) state.expression else "",
        previewText = state.previewResult,
        isResult = state.isResultDisplayed,
        isError = state.isError,
    )

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        // Expression line (shown above result when = was pressed)
        AnimatedContent(
            targetState = displayData.expressionText,
            transitionSpec = {
                (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) { -it } + fadeIn())
                    .togetherWith(
                        slideOutVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) { it } + fadeOut()
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
        AnimatedContent(
            targetState = displayData.mainText,
            transitionSpec = {
                if (targetState != initialState) {
                    (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 300f)) { it } + fadeIn())
                        .togetherWith(
                            slideOutVertically(spring(dampingRatio = 0.7f, stiffness = 300f)) { -it } + fadeOut()
                        )
                } else {
                    fadeIn() togetherWith fadeOut()
                }
            },
            label = "mainDisplay",
        ) { text ->
            Text(
                text = text,
                style = if (text.length > 12) {
                    MaterialTheme.typography.displaySmall
                } else if (text.length > 8) {
                    MaterialTheme.typography.displayMedium
                } else {
                    MaterialTheme.typography.displayLarge
                },
                color = if (displayData.isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Live preview result (shown when typing, not after =)
        AnimatedContent(
            targetState = if (!displayData.isResult && displayData.previewText.isNotEmpty()
                && displayData.previewText != displayData.mainText
            ) {
                displayData.previewText
            } else {
                ""
            },
            transitionSpec = {
                fadeIn(spring(stiffness = 800f)) togetherWith fadeOut(spring(stiffness = 800f))
            },
            label = "preview",
        ) { preview ->
            if (preview.isNotEmpty()) {
                Text(
                    text = "= $preview",
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
