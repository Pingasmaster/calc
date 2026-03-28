package com.calculator.app.ui.calculator.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.calculator.app.R
import com.calculator.app.domain.model.CalculatorState

private const val TEXT_LENGTH_SMALL_THRESHOLD = 12
private const val TEXT_LENGTH_MEDIUM_THRESHOLD = 8

private data class DisplayData(
    val mainText: String,
    val expressionText: String,
    val previewText: String,
    val isResult: Boolean,
    val isError: Boolean,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        val motionScheme = MaterialTheme.motionScheme

        // Expression line (shown above result when = was pressed)
        AnimatedContent(
            targetState = displayData.expressionText,
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
        AnimatedContent(
            targetState = displayData.mainText,
            transitionSpec = {
                if (targetState != initialState) {
                    (slideInVertically(motionScheme.defaultSpatialSpec()) { it } + fadeIn(motionScheme.defaultEffectsSpec()))
                        .togetherWith(
                            slideOutVertically(motionScheme.defaultSpatialSpec()) { -it } + fadeOut(motionScheme.defaultEffectsSpec())
                        )
                } else {
                    fadeIn(motionScheme.defaultEffectsSpec()) togetherWith fadeOut(motionScheme.defaultEffectsSpec())
                }
            },
            label = "mainDisplay",
        ) { text ->
            Text(
                text = text,
                style = if (text.length > TEXT_LENGTH_SMALL_THRESHOLD) {
                    MaterialTheme.typography.displaySmall
                } else if (text.length > TEXT_LENGTH_MEDIUM_THRESHOLD) {
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
