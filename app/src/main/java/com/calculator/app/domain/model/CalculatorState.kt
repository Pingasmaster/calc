package com.calculator.app.domain.model

data class CalculatorState(
    val expression: String = "",
    val displayText: String = "0",
    val previewResult: String = "",
    val isResultDisplayed: Boolean = false,
    val isError: Boolean = false,
    val openParenCount: Int = 0,
)
