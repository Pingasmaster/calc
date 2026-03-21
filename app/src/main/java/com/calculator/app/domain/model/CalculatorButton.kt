package com.calculator.app.domain.model

enum class ButtonCategory {
    NUMBER,
    OPERATOR,
    FUNCTION,
    AC,
    SCIENTIFIC,
    EQUALS,
    BACKSPACE,
}

data class CalculatorButton(
    val symbol: String,
    val category: ButtonCategory,
    val contentDescription: String = symbol,
    val widthWeight: Float = 1f,
)

val scientificRow = listOf(
    CalculatorButton("√", ButtonCategory.SCIENTIFIC, "Square root"),
    CalculatorButton("π", ButtonCategory.SCIENTIFIC, "Pi"),
    CalculatorButton("e", ButtonCategory.SCIENTIFIC, "Euler's number"),
    CalculatorButton("!", ButtonCategory.SCIENTIFIC, "Factorial"),
    CalculatorButton("⌫", ButtonCategory.BACKSPACE, "Backspace"),
)

val buttonRows = listOf(
    listOf(
        CalculatorButton("AC", ButtonCategory.AC, "All clear"),
        CalculatorButton("()", ButtonCategory.FUNCTION, "Parentheses"),
        CalculatorButton("%", ButtonCategory.FUNCTION, "Percent"),
        CalculatorButton("÷", ButtonCategory.OPERATOR, "Divide", widthWeight = 0.75f),
    ),
    listOf(
        CalculatorButton("7", ButtonCategory.NUMBER),
        CalculatorButton("8", ButtonCategory.NUMBER),
        CalculatorButton("9", ButtonCategory.NUMBER),
        CalculatorButton("×", ButtonCategory.OPERATOR, "Multiply", widthWeight = 0.75f),
    ),
    listOf(
        CalculatorButton("4", ButtonCategory.NUMBER),
        CalculatorButton("5", ButtonCategory.NUMBER),
        CalculatorButton("6", ButtonCategory.NUMBER),
        CalculatorButton("−", ButtonCategory.OPERATOR, "Subtract", widthWeight = 0.75f),
    ),
    listOf(
        CalculatorButton("1", ButtonCategory.NUMBER),
        CalculatorButton("2", ButtonCategory.NUMBER),
        CalculatorButton("3", ButtonCategory.NUMBER),
        CalculatorButton("+", ButtonCategory.OPERATOR, "Add", widthWeight = 0.75f),
    ),
    listOf(
        CalculatorButton("0", ButtonCategory.NUMBER),
        CalculatorButton(".", ButtonCategory.NUMBER, "Decimal point"),
        CalculatorButton("=", ButtonCategory.EQUALS, "Equals", widthWeight = 2f),
    ),
)
