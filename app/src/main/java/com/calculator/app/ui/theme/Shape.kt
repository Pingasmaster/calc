package com.calculator.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// Values aligned to M3 shape scale: small=8, medium=12, large=16, extraLarge=28
object CalculatorShapes {
    val Button: Shape = CircleShape
    val ButtonPressed: Shape = RoundedCornerShape(20.dp)
    val WideButton: Shape = CircleShape                    // pill shape (= button)
    val WideButtonPressed: Shape = RoundedCornerShape(16.dp)  // M3 large
    val OperatorButton: Shape = RoundedCornerShape(16.dp)     // M3 large
    val OperatorButtonPressed: Shape = RoundedCornerShape(12.dp) // M3 medium
    val BackspaceButton: Shape = RoundedCornerShape(12.dp)    // M3 medium
    val BackspaceButtonPressed: Shape = RoundedCornerShape(8.dp) // M3 small
    val DisplayPanel: Shape = RoundedCornerShape(28.dp)       // M3 extraLarge
    val HistoryCard: Shape = RoundedCornerShape(16.dp)        // M3 large
    val HistoryOverlay: Shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp) // M3 extraLarge
}

/**
 * Shape for segmented list items — first item has large top corners,
 * last item has large bottom corners, middle items have small corners.
 */
fun segmentedItemShape(index: Int, count: Int): RoundedCornerShape {
    val large = 28.dp  // M3 extraLarge
    val small = 4.dp   // M3 extraSmall
    return when {
        count == 1 -> RoundedCornerShape(large)
        index == 0 -> RoundedCornerShape(topStart = large, topEnd = large, bottomStart = small, bottomEnd = small)
        index == count - 1 -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = large, bottomEnd = large)
        else -> RoundedCornerShape(small)
    }
}
