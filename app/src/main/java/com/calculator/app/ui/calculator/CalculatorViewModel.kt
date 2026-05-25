package com.calculator.app.ui.calculator

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calculator.app.CalculatorApplication
import com.calculator.app.data.repository.HistoryRepository
import com.calculator.app.domain.engine.CalculatorEngine
import com.calculator.app.domain.model.CalculatorState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CalculatorViewModel(
    private val historyRepo: HistoryRepository,
    private val savedStateHandle: SavedStateHandle,
    private val engine: CalculatorEngine = CalculatorEngine(),
    private val previewDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val previewDebounceMs: Long = PREVIEW_DEBOUNCE_MS,
    private val saveStateDebounceMs: Long = SAVE_STATE_DEBOUNCE_MS,
) : ViewModel() {

    companion object {
        private const val KEY_EXPRESSION = "expression"
        private const val KEY_DISPLAY = "displayText"
        private const val KEY_RESULT_DISPLAYED = "isResultDisplayed"
        private const val KEY_IS_ERROR = "isError"

        /**
         * Coalesce rapid typing into one engine evaluation per ~150 ms. Anything
         * under ~200 ms still feels instant to the user but cuts BigDecimal
         * evaluation rate roughly 3x vs the prior 50 ms.
         */
        const val PREVIEW_DEBOUNCE_MS: Long = 150L

        /**
         * SavedStateHandle is only consulted by the system at
         * onSaveInstanceState (which we hook synchronously from
         * [MainActivity]), so we can coalesce per-keystroke writes into one
         * flush per quiet window. Tests pass `0L` to keep writes synchronous.
         */
        const val SAVE_STATE_DEBOUNCE_MS: Long = 500L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalculatorApplication
                val historyRepo = HistoryRepository(app.database.historyDao())
                val savedStateHandle = createSavedStateHandle()
                CalculatorViewModel(historyRepo, savedStateHandle)
            }
        }
    }

    val expressionField = TextFieldState(
        initialText = savedStateHandle[KEY_EXPRESSION] ?: "",
    )

    private val _state = MutableStateFlow(
        run {
            val expr = savedStateHandle[KEY_EXPRESSION] ?: ""
            CalculatorState(
                expression = expr,
                displayText = savedStateHandle[KEY_DISPLAY] ?: "0",
                openParenCount = parenCount(expr),
                isResultDisplayed = savedStateHandle[KEY_RESULT_DISPLAYED] ?: false,
                isError = savedStateHandle[KEY_IS_ERROR] ?: false,
            )
        },
    )
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    val history = historyRepo.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    // Each input method pushes the new expression here; the pipeline below
    // debounces, evaluates on [previewDispatcher], and writes the preview back
    // to `_state`. mapLatest cancels stale work when the user keeps typing.
    private val pendingExpression = MutableStateFlow(expressionField.text.toString())

    // Ticks once per keystroke. A debounced collector batches the SavedStateHandle
    // writes; lifecycle ON_STOP / process death bypass it via [flushSaveStateNow].
    private val saveStateTick = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        startPreviewPipeline()
        startSaveStatePipeline()
    }

    @OptIn(FlowPreview::class)
    private fun startSaveStatePipeline() {
        // saveStateDebounceMs == 0 means "no batching" — used by tests so they
        // can assert on SavedStateHandle synchronously after a click. In that
        // mode `saveState()` writes directly and the collector is unnecessary.
        if (saveStateDebounceMs == 0L) return
        viewModelScope.launch {
            saveStateTick
                .debounce(saveStateDebounceMs)
                .collect { flushSaveState() }
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun startPreviewPipeline() {
        viewModelScope.launch {
            val suppressFlow = _state
                .map { it.isResultDisplayed || it.isError }
                .distinctUntilChanged()

            combine(pendingExpression, suppressFlow) { text, suppress -> text to suppress }
                .debounce(previewDebounceMs)
                .mapLatest { (text, suppress) ->
                    if (text.isEmpty() || suppress) {
                        ""
                    } else {
                        val openCount = parenCount(text)
                        val closedExpr = if (openCount == 0) text else text + ")".repeat(openCount)
                        engine.evaluate(closedExpr).getOrNull() ?: ""
                    }
                }
                .flowOn(previewDispatcher)
                .distinctUntilChanged()
                .collect { preview ->
                    _state.update { it.copy(previewResult = preview) }
                }
        }
    }

    private fun publishExpression() {
        pendingExpression.value = expression
    }

    private val operators = setOf("+", "−", "×", "÷")

    private val expression: String get() = expressionField.text.toString()

    private val cursor: Int get() = expressionField.selection.start

    private fun parenCount(expr: String = expression): Int {
        var open = 0
        var close = 0
        for (c in expr) {
            when (c) {
                '(' -> open++
                ')' -> close++
            }
        }
        return (open - close).coerceAtLeast(0)
    }

    private fun syncState() {
        val expr = expression
        val openCount = parenCount(expr)
        _state.update {
            it.copy(
                expression = expr,
                displayText = expr.ifEmpty { "0" },
                openParenCount = openCount,
                isError = false,
            )
        }
    }

    /**
     * Fires per keystroke. The preview pipeline must see every change immediately,
     * but the SavedStateHandle Bundle writes can wait for the next debounce window
     * (or a lifecycle flush via [flushSaveStateNow]). See [SAVE_STATE_DEBOUNCE_MS].
     */
    private fun saveState() {
        publishExpression()
        if (saveStateDebounceMs == 0L) {
            flushSaveState()
        } else {
            saveStateTick.tryEmit(Unit)
        }
    }

    /**
     * Writes the current state into [SavedStateHandle]. Called by the debounced
     * collector during normal operation and by [flushSaveStateNow] (which the
     * Activity invokes from `onSaveInstanceState` / lifecycle ON_STOP).
     */
    private fun flushSaveState() {
        val s = _state.value
        savedStateHandle[KEY_EXPRESSION] = if (s.isResultDisplayed) s.expression else expression
        savedStateHandle[KEY_DISPLAY] = s.displayText
        savedStateHandle[KEY_RESULT_DISPLAYED] = s.isResultDisplayed
        savedStateHandle[KEY_IS_ERROR] = s.isError
    }

    /** Lifecycle entry point: force an immediate flush, bypassing the debounce. */
    fun flushSaveStateNow() {
        flushSaveState()
    }

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
        val current = _state.value
        if (current.isResultDisplayed || current.isError) {
            expressionField.setTextAndPlaceCursorAtEnd(digit)
            _state.update { CalculatorState(expression = digit, displayText = digit) }
        } else {
            if (digit == ".") {
                val pos = cursor
                val text = expression
                val charBefore = if (pos > 0) text[pos - 1] else null
                if (charBefore == 'π' || charBefore == 'e' || charBefore == '%' || charBefore == '!') return
                // Walk outward from the cursor through the current number;
                // abort if a '.' is already inside it. Avoids allocating
                // two substrings + a concat just to call `'.' in (left + right)`.
                var i = pos - 1
                while (i >= 0) {
                    val c = text[i]
                    if (c == '.') return
                    if (!c.isDigit()) break
                    i--
                }
                var j = pos
                while (j < text.length) {
                    val c = text[j]
                    if (c == '.') return
                    if (!c.isDigit()) break
                    j++
                }
            }
            expressionField.edit {
                val pos = selection.start
                insert(pos, digit)
                selection = TextRange(pos + digit.length)
            }
            syncState()
        }
        saveState()
    }

    private fun appendOperator(op: String) {
        val current = _state.value
        if (current.isError) {
            expressionField.clearText()
            _state.update { CalculatorState() }
            saveState()
            return
        }

        if (current.isResultDisplayed) {
            val result = current.displayText
            expressionField.setTextAndPlaceCursorAtEnd(result + op)
            _state.update {
                val newExpr = result + op
                CalculatorState(expression = newExpr, displayText = newExpr)
            }
        } else {
            val text = expression
            if (text.isEmpty()) return

            val pos = cursor
            if (pos == text.length) {
                val lastChar = text.last().toString()
                if (lastChar in operators) {
                    expressionField.edit {
                        delete(length - 1, length)
                        append(op)
                        placeCursorAtEnd()
                    }
                } else {
                    expressionField.edit {
                        append(op)
                        placeCursorAtEnd()
                    }
                }
            } else {
                expressionField.edit {
                    insert(pos, op)
                    selection = TextRange(pos + op.length)
                }
            }
            syncState()
        }
        saveState()
    }

    private fun appendConstant(constant: String) {
        val current = _state.value
        if (current.isResultDisplayed || current.isError) {
            expressionField.setTextAndPlaceCursorAtEnd(constant)
            _state.update { CalculatorState(expression = constant, displayText = constant) }
        } else {
            expressionField.edit {
                val pos = selection.start
                insert(pos, constant)
                selection = TextRange(pos + constant.length)
            }
            syncState()
        }
        saveState()
    }

    private fun appendSqrt() {
        val current = _state.value
        if (current.isResultDisplayed || current.isError) {
            expressionField.setTextAndPlaceCursorAtEnd("√(")
            _state.update { CalculatorState(expression = "√(", displayText = "√(", openParenCount = 1) }
        } else {
            expressionField.edit {
                val pos = selection.start
                insert(pos, "√(")
                selection = TextRange(pos + 2)
            }
            syncState()
        }
        saveState()
    }

    private fun appendToExpression(symbol: String) {
        val current = _state.value
        if (current.isError) {
            expressionField.clearText()
            _state.update { CalculatorState() }
            saveState()
            return
        }
        if (!current.isResultDisplayed && expression.isEmpty()) return

        if (current.isResultDisplayed) {
            val newExpr = current.displayText + symbol
            expressionField.setTextAndPlaceCursorAtEnd(newExpr)
            _state.update { CalculatorState(expression = newExpr, displayText = newExpr) }
        } else {
            expressionField.edit {
                val pos = selection.start
                insert(pos, symbol)
                selection = TextRange(pos + symbol.length)
            }
            syncState()
        }
        saveState()
    }

    private fun toggleParentheses() {
        val current = _state.value

        if (current.isResultDisplayed || current.isError) {
            expressionField.setTextAndPlaceCursorAtEnd("(")
            _state.update { CalculatorState(expression = "(", displayText = "(", openParenCount = 1) }
            saveState()
            return
        }

        val text = expression
        val pos = if (text.isEmpty()) 0 else cursor
        val charBefore = if (pos > 0 && text.isNotEmpty()) text[pos - 1] else null

        val shouldClose = parenCount() > 0 &&
            text.isNotEmpty() &&
            charBefore != null &&
            charBefore.toString() !in operators &&
            charBefore != '('

        if (shouldClose) {
            expressionField.edit {
                insert(pos, ")")
                selection = TextRange(pos + 1)
            }
        } else {
            val prefix = if (charBefore != null &&
                (
                    charBefore.isDigit() || charBefore == ')' || charBefore == 'π' || charBefore == 'e' || charBefore == '%' ||
                        charBefore == '!'
                    )
            ) {
                "×("
            } else {
                "("
            }
            expressionField.edit {
                insert(pos, prefix)
                selection = TextRange(pos + prefix.length)
            }
        }
        syncState()
        saveState()
    }

    private fun onEquals() {
        var expr = expression

        if (expr.isEmpty()) return

        // Strip trailing operators in a single substring instead of N dropLast() allocations.
        var end = expr.length
        while (end > 0 && expr[end - 1].toString() in operators) {
            end--
        }
        if (end == 0) return
        if (end < expr.length) expr = expr.substring(0, end)

        val openParens = parenCount(expr)
        val closedExpr = expr + ")".repeat(openParens)

        engine.evaluate(closedExpr).fold(
            onSuccess = { result ->
                expressionField.setTextAndPlaceCursorAtEnd(closedExpr)
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
                saveState()
                viewModelScope.launch {
                    // Safety net for unknown DB/repo failures: room can throw
                    // SQLiteException, IllegalStateException, IOException, or
                    // any wrapped variant — listing each adds no signal. We
                    // explicitly rethrow CancellationException so coroutine
                    // cancellation propagates correctly (the historical
                    // runCatching here would have swallowed it).
                    @Suppress("TooGenericExceptionCaught")
                    try {
                        historyRepo.addEntry(closedExpr, result)
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (t: Throwable) {
                        android.util.Log.w("CalculatorApp", "addEntry failed", t)
                    }
                }
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
                saveState()
            },
        )
    }

    private fun onClear() {
        expressionField.clearText()
        _state.update { CalculatorState() }
        saveState()
    }

    private fun onBackspace() {
        val current = _state.value
        if (current.isResultDisplayed || current.isError) {
            expressionField.clearText()
            _state.update { CalculatorState() }
            saveState()
            return
        }

        val text = expression
        val pos = cursor
        if (text.isEmpty() || pos == 0) return

        val removed = text[pos - 1]
        expressionField.edit {
            delete(pos - 1, pos)
            if (removed == '(' && pos >= 2 && text[pos - 2] == '√') {
                delete(pos - 2, pos - 1)
                selection = TextRange(pos - 2)
            } else {
                selection = TextRange(pos - 1)
            }
        }
        syncState()
        saveState()
    }

    fun clearHistory() {
        viewModelScope.launch {
            // Safety net for unknown DB/repo failures. See addEntry above for
            // rationale on the generic catch + explicit cancellation rethrow.
            @Suppress("TooGenericExceptionCaught")
            try {
                historyRepo.clearHistory()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                android.util.Log.w("CalculatorApp", "clearHistory failed", t)
            }
        }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            // Safety net for unknown DB/repo failures. See addEntry above for
            // rationale on the generic catch + explicit cancellation rethrow.
            @Suppress("TooGenericExceptionCaught")
            try {
                historyRepo.deleteEntry(id)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                android.util.Log.w("CalculatorApp", "deleteEntry failed", t)
            }
        }
    }

    fun loadFromHistory(value: String) {
        expressionField.setTextAndPlaceCursorAtEnd(value)
        _state.update {
            CalculatorState(
                expression = value,
                displayText = value,
                openParenCount = parenCount(value),
            )
        }
        saveState()
    }
}
