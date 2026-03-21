package com.calculator.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object CalculatorShapes {
    val Button: Shape = CircleShape
    val ButtonPressed: Shape = RoundedCornerShape(20.dp)
    val WideButton: Shape = RoundedCornerShape(50)
    val WideButtonPressed: Shape = RoundedCornerShape(16.dp)
    val OperatorButton: Shape = RoundedCornerShape(16.dp)
    val OperatorButtonPressed: Shape = RoundedCornerShape(12.dp)
    val BackspaceButton: Shape = RoundedCornerShape(12.dp)
    val BackspaceButtonPressed: Shape = RoundedCornerShape(8.dp)
    val DisplayPanel: Shape = RoundedCornerShape(28.dp)
    val HistoryCard: Shape = RoundedCornerShape(16.dp)
    val HistoryOverlay: Shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
}

/**
 * Shape for segmented list items — first item has large top corners,
 * last item has large bottom corners, middle items have small corners.
 */
fun segmentedItemShape(index: Int, count: Int): RoundedCornerShape {
    val large = 28.dp
    val small = 4.dp
    return when {
        count == 1 -> RoundedCornerShape(large)
        index == 0 -> RoundedCornerShape(topStart = large, topEnd = large, bottomStart = small, bottomEnd = small)
        index == count - 1 -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = large, bottomEnd = large)
        else -> RoundedCornerShape(small)
    }
}
