package com.calculator.app.ui.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calculator.app.CalculatorApplication
import com.calculator.app.data.repository.HistoryRepository
import com.calculator.app.domain.engine.CalculatorEngine
import com.calculator.app.domain.model.CalculatorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as CalculatorApplication).database
    private val historyRepo = HistoryRepository(db.historyDao())
    private val engine = CalculatorEngine()

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    val history = historyRepo.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val operators = setOf("+", "−", "×", "÷")

    fun onButtonClick(symbol: String) {
        when (symbol) {
            "AC" -> onClear()
            "⌫" -> onBackspace()
            "=" -> onEquals()
            "()" -> toggleParentheses()
            "%" -> appendToExpression("%")
            "!" -> appendToExpression("!")
            "√" -> appendSqrt()
            "π" -> appendConstant("π")
            "e" -> appendConstant("e")
            in operators -> appendOperator(symbol)
            else -> appendDigit(symbol)
        }
    }

    private fun appendDigit(digit: String) {
        _state.update { current ->
            if (current.isResultDisplayed) {
                // Start fresh expression with the digit
                CalculatorState(expression = digit, displayText = digit)
            } else {
                // Prevent decimal after constants or postfix operators
                if (digit == ".") {
                    val lastChar = current.expression.lastOrNull()
                    if (lastChar == 'π' || lastChar == 'e' || lastChar == '%' || lastChar == '!') return@update current
                    val currentNumber = current.expression.takeLastWhile { it.isDigit() || it == '.' }
                    if ('.' in currentNumber) return@update current
                }
                val newExpr = current.expression + digit
                current.copy(
                    expression = newExpr,
                    displayText = newExpr,
                    isError = false,
                )
            }
        }
        updatePreview()
    }

    private fun appendOperator(op: String) {
        _state.update { current ->
            if (current.isError) return@update CalculatorState()

            val expr = if (current.isResultDisplayed) {
                // Continue from result
                current.displayText
            } else {
                current.expression
            }

            if (expr.isEmpty()) return@update current

            // Replace last operator if there is one
            val lastChar = expr.last().toString()
            val newExpr = if (lastChar in operators) {
                expr.dropLast(1) + op
            } else {
                expr + op
            }

            current.copy(
                expression = newExpr,
                displayText = newExpr,
                isResultDisplayed = false,
                isError = false,
            )
        }
        updatePreview()
    }

    private fun appendConstant(constant: String) {
        _state.update { current ->
            if (current.isResultDisplayed) {
                CalculatorState(expression = constant, displayText = constant)
            } else {
                val newExpr = current.expression + constant
                current.copy(
                    expression = newExpr,
                    displayText = newExpr,
                    isError = false,
                )
            }
        }
        updatePreview()
    }

    private fun appendSqrt() {
        _state.update { current ->
            if (current.isResultDisplayed) {
                CalculatorState(expression = "√(", displayText = "√(", openParenCount = 1)
            } else {
                val newExpr = current.expression + "√("
                current.copy(
                    expression = newExpr,
                    displayText = newExpr,
                    openParenCount = current.openParenCount + 1,
                    isError = false,
                )
            }
        }
        updatePreview()
    }

    private fun appendToExpression(symbol: String) {
        _state.update { current ->
            if (current.isError) return@update CalculatorState()

            if (current.isResultDisplayed) {
                val newExpr = current.displayText + symbol
                CalculatorState(expression = newExpr, displayText = newExpr)
            } else {
                val newExpr = current.expression + symbol
                current.copy(
                    expression = newExpr,
                    displayText = newExpr,
                    isError = false,
                )
            }
        }
        updatePreview()
    }

    private fun toggleParentheses() {
        _state.update { current ->
            val expr = if (current.isResultDisplayed) "" else current.expression

            // Smart parentheses: close if there are unclosed parens and it makes sense
            val shouldClose = current.openParenCount > 0 &&
                    expr.isNotEmpty() &&
                    expr.last().toString() !in operators &&
                    expr.last() != '('

            val (newExpr, newParenCount) = if (shouldClose) {
                (expr + ")") to (current.openParenCount - 1)
            } else {
                // Add implicit multiplication if needed
                val prefix = if (expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == ')' || expr.last() == 'π' || expr.last() == 'e' || expr.last() == '%' || expr.last() == '!')) {
                    expr + "×"
                } else {
                    expr
                }
                (prefix + "(") to (current.openParenCount + 1)
            }

            current.copy(
                expression = newExpr,
                displayText = newExpr,
                isResultDisplayed = false,
                isError = false,
                openParenCount = newParenCount,
            )
        }
        updatePreview()
    }

    private fun onEquals() {
        val currentState = _state.value
        val expr = currentState.expression

        if (expr.isEmpty()) return

        // Auto-close unclosed parentheses
        val closedExpr = expr + ")".repeat(currentState.openParenCount)

        engine.evaluate(closedExpr).fold(
            onSuccess = { result ->
                _state.update {
                    it.copy(
                        expression = closedExpr,
                        displayText = result,
                        previewResult = "",
                        isResultDisplayed = true,
                        isError = false,
                        openParenCount = 0,
                    )
                }
                viewModelScope.launch { historyRepo.addEntry(closedExpr, result) }
            },
            onFailure = {
                _state.update {
                    it.copy(
                        displayText = "Error",
                        previewResult = "",
                        isError = true,
                        isResultDisplayed = true,
                        openParenCount = 0,
                    )
                }
            },
        )
    }

    private fun onClear() {
        _state.update { CalculatorState() }
    }

    private fun onBackspace() {
        _state.update { current ->
            if (current.isResultDisplayed || current.isError) {
                return@update CalculatorState()
            }

            if (current.expression.isEmpty()) return@update current

            val removed = current.expression.last()
            var newExpr = current.expression.dropLast(1)

            var newParenCount = when (removed) {
                '(' -> current.openParenCount - 1
                ')' -> current.openParenCount + 1
                else -> current.openParenCount
            }

            // Remove lone √ left behind when deleting the ( from √(
            if (removed == '(' && newExpr.endsWith("√")) {
                newExpr = newExpr.dropLast(1)
            }

            current.copy(
                expression = newExpr,
                displayText = newExpr.ifEmpty { "0" },
                openParenCount = newParenCount.coerceAtLeast(0),
                isError = false,
            )
        }
        updatePreview()
    }

    private fun updatePreview() {
        val expr = _state.value.expression
        if (expr.isEmpty() || _state.value.isResultDisplayed) {
            _state.update { it.copy(previewResult = "") }
            return
        }

        // Auto-close parens for preview
        val closedExpr = expr + ")".repeat(_state.value.openParenCount)

        engine.evaluate(closedExpr).fold(
            onSuccess = { preview ->
                _state.update { it.copy(previewResult = preview) }
            },
            onFailure = {
                _state.update { it.copy(previewResult = "") }
            },
        )
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepo.clearHistory() }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch { historyRepo.deleteEntry(id) }
    }

    fun loadFromHistory(value: String) {
        val parenCount = (value.count { it == '(' } - value.count { it == ')' }).coerceAtLeast(0)
        _state.update {
            CalculatorState(
                expression = value,
                displayText = value,
                openParenCount = parenCount,
            )
        }
        updatePreview()
    }
}
