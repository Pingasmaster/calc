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
