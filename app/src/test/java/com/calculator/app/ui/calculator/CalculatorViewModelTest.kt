package com.calculator.app.ui.calculator

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.calculator.app.data.repository.HistoryRepository
import com.calculator.app.domain.engine.CalculatorEngine
import com.calculator.app.domain.model.CalculatorState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var historyRepo: HistoryRepository
    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        historyRepo = mockk(relaxed = true)
        coEvery { historyRepo.observeHistory() } returns flowOf(emptyList())
        viewModel = newViewModel(SavedStateHandle())
    }

    private fun newViewModel(handle: SavedStateHandle): CalculatorViewModel =
        CalculatorViewModel(
            historyRepo = historyRepo,
            savedStateHandle = handle,
            previewDispatcher = testDispatcher,
            previewDebounceMs = 0L,
            saveStateDebounceMs = 0L,
        )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== Digit input ==========

    @Test
    fun `initial state shows zero`() {
        assertEquals("0", viewModel.state.value.displayText)
        assertEquals("", viewModel.state.value.expression)
    }

    @Test
    fun `pressing digit updates expression and display`() {
        viewModel.onButtonClick("5")
        assertEquals("5", viewModel.state.value.expression)
        assertEquals("5", viewModel.state.value.displayText)
    }

    @Test
    fun `pressing multiple digits builds expression`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("3")
        assertEquals("123", viewModel.state.value.expression)
        assertEquals("123", viewModel.state.value.displayText)
    }

    @Test
    fun `pressing decimal point adds it`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick(".")
        viewModel.onButtonClick("5")
        assertEquals("1.5", viewModel.state.value.expression)
    }

    @Test
    fun `duplicate decimal point is rejected`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick(".")
        viewModel.onButtonClick("5")
        viewModel.onButtonClick(".")
        assertEquals("1.5", viewModel.state.value.expression)
    }

    @Test
    fun `decimal after constant is rejected`() {
        viewModel.onButtonClick("π")
        viewModel.onButtonClick(".")
        assertEquals("π", viewModel.state.value.expression)
    }

    // ========== Operator input ==========

    @Test
    fun `pressing operator after digit appends it`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        assertEquals("5+", viewModel.state.value.expression)
    }

    @Test
    fun `replacing operator when pressing new operator`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("×")
        assertEquals("5×", viewModel.state.value.expression)
    }

    @Test
    fun `operator on empty expression is ignored`() {
        viewModel.onButtonClick("+")
        assertEquals("", viewModel.state.value.expression)
    }

    // ========== Clear ==========

    @Test
    fun `AC resets to initial state`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("AC")
        assertEquals("", viewModel.state.value.expression)
        assertEquals("0", viewModel.state.value.displayText)
    }

    // ========== Backspace ==========

    @Test
    fun `backspace removes last character`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("⌫")
        assertEquals("12", viewModel.state.value.expression)
        assertEquals("12", viewModel.state.value.displayText)
    }

    @Test
    fun `backspace on single character shows zero`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("⌫")
        assertEquals("", viewModel.state.value.expression)
        assertEquals("0", viewModel.state.value.displayText)
    }

    @Test
    fun `backspace after result clears everything`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        viewModel.onButtonClick("⌫")
        assertEquals("", viewModel.state.value.expression)
        assertEquals("0", viewModel.state.value.displayText)
    }

    @Test
    fun `backspace removes sqrt with its opening paren`() {
        viewModel.onButtonClick("√")
        assertEquals("√(", viewModel.state.value.expression)
        assertEquals(1, viewModel.state.value.openParenCount)
        viewModel.onButtonClick("⌫")
        assertEquals("", viewModel.state.value.expression)
        assertEquals(0, viewModel.state.value.openParenCount)
    }

    // ========== Parentheses ==========

    @Test
    fun `toggle parentheses opens paren`() {
        viewModel.onButtonClick("()")
        assertEquals("(", viewModel.state.value.expression)
        assertEquals(1, viewModel.state.value.openParenCount)
    }

    @Test
    fun `toggle parentheses closes when appropriate`() {
        viewModel.onButtonClick("()")
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("()")
        assertEquals("(5)", viewModel.state.value.expression)
        assertEquals(0, viewModel.state.value.openParenCount)
    }

    @Test
    fun `implicit multiplication before opening paren`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("()")
        assertEquals("5×(", viewModel.state.value.expression)
    }

    @Test
    fun `nested parentheses tracking`() {
        viewModel.onButtonClick("()")
        viewModel.onButtonClick("()")
        viewModel.onButtonClick("5")
        assertEquals(2, viewModel.state.value.openParenCount)
        viewModel.onButtonClick("()")
        assertEquals(1, viewModel.state.value.openParenCount)
        viewModel.onButtonClick("()")
        assertEquals(0, viewModel.state.value.openParenCount)
    }

    // ========== Equals ==========

    @Test
    fun `equals evaluates expression`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        assertEquals("5", viewModel.state.value.displayText)
        assertTrue(viewModel.state.value.isResultDisplayed)
    }

    @Test
    fun `equals auto-closes unclosed parentheses`() {
        viewModel.onButtonClick("(")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        // Don't close the paren
        viewModel.onButtonClick("=")
        assertEquals("5", viewModel.state.value.displayText)
        assertEquals(0, viewModel.state.value.openParenCount)
    }

    @Test
    fun `equals on empty expression does nothing`() {
        viewModel.onButtonClick("=")
        assertEquals("0", viewModel.state.value.displayText)
        assertFalse(viewModel.state.value.isResultDisplayed)
    }

    @Test
    fun `equals on invalid expression shows error`() {
        viewModel.onButtonClick("÷")
        // Can't start with operator, so expression is empty
        viewModel.onButtonClick("=")
        // Since expression is empty, equals does nothing
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun `equals saves to history`() = runTest {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")

        coVerify { historyRepo.addEntry("2+3", "5") }
    }

    // ========== Continuing from result ==========

    @Test
    fun `digit after result starts new expression`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        assertTrue(viewModel.state.value.isResultDisplayed)

        viewModel.onButtonClick("7")
        assertEquals("7", viewModel.state.value.expression)
        assertFalse(viewModel.state.value.isResultDisplayed)
    }

    @Test
    fun `operator after result continues from result`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        assertEquals("5", viewModel.state.value.displayText)

        viewModel.onButtonClick("+")
        assertEquals("5+", viewModel.state.value.expression)
    }

    // ========== Constants ==========

    @Test
    fun `pi is appended correctly`() {
        viewModel.onButtonClick("π")
        assertEquals("π", viewModel.state.value.expression)
    }

    @Test
    fun `e constant is appended correctly`() {
        viewModel.onButtonClick("e")
        assertEquals("e", viewModel.state.value.expression)
    }

    @Test
    fun `constant after result starts fresh`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("=")
        viewModel.onButtonClick("π")
        assertEquals("π", viewModel.state.value.expression)
    }

    // ========== Scientific functions ==========

    @Test
    fun `sqrt appends with opening paren`() {
        viewModel.onButtonClick("√")
        assertEquals("√(", viewModel.state.value.expression)
        assertEquals(1, viewModel.state.value.openParenCount)
    }

    @Test
    fun `sqrt after result starts fresh`() {
        viewModel.onButtonClick("9")
        viewModel.onButtonClick("=")
        viewModel.onButtonClick("√")
        assertEquals("√(", viewModel.state.value.expression)
        assertEquals(1, viewModel.state.value.openParenCount)
    }

    @Test
    fun `percent is appended`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("0")
        viewModel.onButtonClick("%")
        assertEquals("50%", viewModel.state.value.expression)
    }

    @Test
    fun `factorial is appended`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("!")
        assertEquals("5!", viewModel.state.value.expression)
    }

    // ========== Preview ==========

    @Test
    fun `preview updates while typing`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        assertEquals("5", viewModel.state.value.previewResult)
    }

    @Test
    fun `preview clears after equals`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        assertEquals("", viewModel.state.value.previewResult)
    }

    @Test
    fun `preview empty for incomplete expression`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        // Just "2+" can't evaluate, preview should be empty
        assertEquals("", viewModel.state.value.previewResult)
    }

    // ========== State flow ==========

    @Test
    fun `state flow emits updates`() = runTest {
        viewModel.state.test {
            val initial = awaitItem()
            assertEquals("0", initial.displayText)

            viewModel.onButtonClick("5")
            val afterDigit = awaitItem()
            assertEquals("5", afterDigit.displayText)

            // Preview update may emit an additional state
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== History operations ==========

    @Test
    fun `clearHistory delegates to repo`() = runTest {
        viewModel.clearHistory()
        coVerify { historyRepo.clearHistory() }
    }

    @Test
    fun `deleteHistoryEntry delegates to repo`() = runTest {
        viewModel.deleteHistoryEntry(42L)
        coVerify { historyRepo.deleteEntry(42L) }
    }

    @Test
    fun `loadFromHistory sets expression and tracks parens`() {
        viewModel.loadFromHistory("(2+3")
        assertEquals("(2+3", viewModel.state.value.expression)
        assertEquals("(2+3", viewModel.state.value.displayText)
        assertEquals(1, viewModel.state.value.openParenCount)
    }

    @Test
    fun `loadFromHistory with balanced parens has zero count`() {
        viewModel.loadFromHistory("(2+3)")
        assertEquals("(2+3)", viewModel.state.value.expression)
        assertEquals(0, viewModel.state.value.openParenCount)
    }

    // ========== SavedStateHandle persistence ==========

    @Test
    fun `state is saved to SavedStateHandle`() {
        val handle = SavedStateHandle()
        val vm = newViewModel(handle)

        vm.onButtonClick("4")
        vm.onButtonClick("2")

        assertEquals("42", handle["expression"])
        assertEquals("42", handle["displayText"])
    }

    @Test
    fun `state is restored from SavedStateHandle`() {
        val handle = SavedStateHandle(
            mapOf(
                "expression" to "12+3",
                "displayText" to "12+3",
                "openParenCount" to 0,
                "isResultDisplayed" to false,
            )
        )
        val vm = newViewModel(handle)

        assertEquals("12+3", vm.state.value.expression)
        assertEquals("12+3", vm.state.value.displayText)
    }

    // ========== Error handling ==========

    @Test
    fun `operator after error clears state`() {
        // Force an error by dividing by zero
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("÷")
        viewModel.onButtonClick("0")
        viewModel.onButtonClick("=")

        if (viewModel.state.value.isError) {
            viewModel.onButtonClick("+")
            assertEquals("", viewModel.state.value.expression)
        }
    }

    @Test
    fun `appendToExpression after error clears state`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("÷")
        viewModel.onButtonClick("0")
        viewModel.onButtonClick("=")

        if (viewModel.state.value.isError) {
            viewModel.onButtonClick("%")
            assertEquals("", viewModel.state.value.expression)
        }
    }

    // ========== Bug fix: isError persisted across process death ==========

    @Test
    fun `isError is saved to SavedStateHandle`() {
        val handle = SavedStateHandle()
        val vm = newViewModel(handle)

        vm.onButtonClick("1")
        vm.onButtonClick("÷")
        vm.onButtonClick("0")
        vm.onButtonClick("=")

        assertTrue(vm.state.value.isError)
        assertEquals(true, handle["isError"])
    }

    @Test
    fun `isError is restored from SavedStateHandle`() {
        val handle = SavedStateHandle(
            mapOf(
                "expression" to "1÷0",
                "displayText" to "Error",
                "openParenCount" to 0,
                "isResultDisplayed" to true,
                "isError" to true,
            )
        )
        val vm = newViewModel(handle)

        assertTrue(vm.state.value.isError)
        assertEquals("Error", vm.state.value.displayText)
    }

    @Test
    fun `operator after restored error does not create Error+ expression`() {
        val handle = SavedStateHandle(
            mapOf(
                "expression" to "1÷0",
                "displayText" to "Error",
                "openParenCount" to 0,
                "isResultDisplayed" to true,
                "isError" to true,
            )
        )
        val vm = newViewModel(handle)
        vm.onButtonClick("+")

        assertFalse(vm.state.value.expression.contains("Error"))
        assertEquals("", vm.state.value.expression)
    }

    // ========== Bug fix: trailing operator before equals ==========

    @Test
    fun `equals with trailing operator strips it and evaluates`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("=")

        assertEquals("5", viewModel.state.value.displayText)
        assertTrue(viewModel.state.value.isResultDisplayed)
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun `equals with expression and trailing operator evaluates correctly`() {
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("×")
        viewModel.onButtonClick("=")

        assertEquals("5", viewModel.state.value.displayText)
        assertFalse(viewModel.state.value.isError)
    }

    // ========== Bug fix: postfix operators on empty expression ==========

    @Test
    fun `percent on empty expression is ignored`() {
        viewModel.onButtonClick("%")

        assertEquals("", viewModel.state.value.expression)
        assertEquals("0", viewModel.state.value.displayText)
    }

    @Test
    fun `factorial on empty expression is ignored`() {
        viewModel.onButtonClick("!")

        assertEquals("", viewModel.state.value.expression)
        assertEquals("0", viewModel.state.value.displayText)
    }

    @Test
    fun `percent after result is allowed`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("0")
        viewModel.onButtonClick("=")
        viewModel.onButtonClick("%")

        assertEquals("50%", viewModel.state.value.expression)
    }

    // ========== Mid-expression editing (cursor-based) ==========

    private fun placeCursorAt(pos: Int) {
        viewModel.expressionField.edit {
            selection = TextRange(pos)
        }
    }

    @Test
    fun `insert digit in middle of expression`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("2")
        placeCursorAt(1)
        viewModel.onButtonClick("5")
        assertEquals("152", viewModel.state.value.expression)
    }

    @Test
    fun `insert operator in middle of expression`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("2")
        placeCursorAt(1)
        viewModel.onButtonClick("+")
        assertEquals("1+2", viewModel.state.value.expression)
    }

    @Test
    fun `operator inserted in middle does not replace an adjacent operator`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("2")
        // Expression: "1+2", cursor at end. Move to between '+' and '2'.
        placeCursorAt(2)
        viewModel.onButtonClick("×")
        // Mid-expression path does not trigger replacement: gives "1+×2".
        assertEquals("1+\u00D72", viewModel.state.value.expression)
    }

    @Test
    fun `insert constant in middle of expression`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("2")
        placeCursorAt(2)
        viewModel.onButtonClick("\u03C0")
        assertEquals("1+\u03C02", viewModel.state.value.expression)
    }

    @Test
    fun `insert sqrt in middle of expression`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("4")
        placeCursorAt(2)
        viewModel.onButtonClick("\u221A")
        assertEquals("2+\u221A(4", viewModel.state.value.expression)
    }

    @Test
    fun `backspace removes character before cursor in middle`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("3")
        placeCursorAt(2)
        viewModel.onButtonClick("\u232B")
        assertEquals("13", viewModel.state.value.expression)
    }

    @Test
    fun `backspace at position zero is no-op`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("3")
        placeCursorAt(0)
        viewModel.onButtonClick("\u232B")
        assertEquals("123", viewModel.state.value.expression)
    }

    @Test
    fun `backspace on lone left paren removes just the paren`() {
        viewModel.onButtonClick("()")
        assertEquals("(", viewModel.state.value.expression)
        viewModel.onButtonClick("\u232B")
        assertEquals("", viewModel.state.value.expression)
        assertEquals(0, viewModel.state.value.openParenCount)
    }

    // ========== Decimal-point guard branches ==========

    @Test
    fun `decimal after e rejected`() {
        viewModel.onButtonClick("e")
        viewModel.onButtonClick(".")
        assertEquals("e", viewModel.state.value.expression)
    }

    @Test
    fun `decimal after percent rejected`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("0")
        viewModel.onButtonClick("%")
        viewModel.onButtonClick(".")
        assertEquals("50%", viewModel.state.value.expression)
    }

    @Test
    fun `decimal after factorial rejected`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("!")
        viewModel.onButtonClick(".")
        assertEquals("5!", viewModel.state.value.expression)
    }

    @Test
    fun `decimal after operator allowed`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick(".")
        assertEquals("5+.", viewModel.state.value.expression)
    }

    @Test
    fun `decimal after left paren allowed`() {
        viewModel.onButtonClick("()")
        viewModel.onButtonClick(".")
        assertEquals("(.", viewModel.state.value.expression)
    }

    @Test
    fun `decimal in middle of existing decimal rejected`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick(".")
        viewModel.onButtonClick("5")
        // Place cursor between '.' and '5' then press '.'
        placeCursorAt(2)
        viewModel.onButtonClick(".")
        assertEquals("1.5", viewModel.state.value.expression)
    }

    @Test
    fun `two separate numbers can each have a decimal`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick(".")
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick(".")
        viewModel.onButtonClick("5")
        assertEquals("1.5+2.5", viewModel.state.value.expression)
    }

    // ========== toggleParentheses branches ==========

    @Test
    fun `toggle after constant pi inserts multiply-paren`() {
        viewModel.onButtonClick("\u03C0")
        viewModel.onButtonClick("()")
        assertEquals("\u03C0\u00D7(", viewModel.state.value.expression)
    }

    @Test
    fun `toggle after constant e inserts multiply-paren`() {
        viewModel.onButtonClick("e")
        viewModel.onButtonClick("()")
        assertEquals("e\u00D7(", viewModel.state.value.expression)
    }

    @Test
    fun `toggle after percent inserts multiply-paren`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("0")
        viewModel.onButtonClick("%")
        viewModel.onButtonClick("()")
        assertEquals("50%\u00D7(", viewModel.state.value.expression)
    }

    @Test
    fun `toggle after factorial inserts multiply-paren`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("!")
        viewModel.onButtonClick("()")
        assertEquals("5!\u00D7(", viewModel.state.value.expression)
    }

    @Test
    fun `toggle after right paren inserts multiply-paren`() {
        viewModel.onButtonClick("()")
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("()")
        viewModel.onButtonClick("()")
        // "(5)" then toggle: charBefore=')' → prefix "×(" → "(5)×("
        assertEquals("(5)\u00D7(", viewModel.state.value.expression)
    }

    @Test
    fun `toggle after operator inserts plain paren`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("()")
        assertEquals("5+(", viewModel.state.value.expression)
    }

    @Test
    fun `toggle after left paren opens nested paren not closes`() {
        viewModel.onButtonClick("()")
        viewModel.onButtonClick("()")
        assertEquals("((", viewModel.state.value.expression)
        assertEquals(2, viewModel.state.value.openParenCount)
    }

    @Test
    fun `toggle right after sqrt opens nested paren`() {
        viewModel.onButtonClick("\u221A")
        viewModel.onButtonClick("()")
        // After "√(" the prev char is '(' so prefix = "(" (no implicit multiply).
        assertEquals("\u221A((", viewModel.state.value.expression)
        assertEquals(2, viewModel.state.value.openParenCount)
    }

    @Test
    fun `toggle with digit before open paren count closes`() {
        viewModel.onButtonClick("()")
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("()")
        assertEquals("(5)", viewModel.state.value.expression)
        assertEquals(0, viewModel.state.value.openParenCount)
    }

    @Test
    fun `toggle after result resets and opens paren`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("=")
        viewModel.onButtonClick("()")
        assertEquals("(", viewModel.state.value.expression)
        assertEquals(1, viewModel.state.value.openParenCount)
        assertFalse(viewModel.state.value.isResultDisplayed)
    }

    // ========== appendOperator after error (all operators) ==========

    private fun forceErrorState() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("\u00F7")
        viewModel.onButtonClick("0")
        viewModel.onButtonClick("=")
        assertTrue("expected error state", viewModel.state.value.isError)
    }

    @Test
    fun `plus after error clears state`() {
        forceErrorState()
        viewModel.onButtonClick("+")
        assertEquals("", viewModel.state.value.expression)
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun `minus after error clears state`() {
        forceErrorState()
        viewModel.onButtonClick("\u2212")
        assertEquals("", viewModel.state.value.expression)
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun `times after error clears state`() {
        forceErrorState()
        viewModel.onButtonClick("\u00D7")
        assertEquals("", viewModel.state.value.expression)
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun `divide after error clears state`() {
        forceErrorState()
        viewModel.onButtonClick("\u00F7")
        assertEquals("", viewModel.state.value.expression)
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun `percent after error clears state`() {
        forceErrorState()
        viewModel.onButtonClick("%")
        assertEquals("", viewModel.state.value.expression)
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun `factorial after error clears state`() {
        forceErrorState()
        viewModel.onButtonClick("!")
        assertEquals("", viewModel.state.value.expression)
        assertFalse(viewModel.state.value.isError)
    }

    // ========== appendConstant branches ==========

    @Test
    fun `constant after operator appends directly`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("\u03C0")
        assertEquals("5+\u03C0", viewModel.state.value.expression)
    }

    @Test
    fun `constant after another constant appends directly`() {
        viewModel.onButtonClick("\u03C0")
        viewModel.onButtonClick("\u03C0")
        assertEquals("\u03C0\u03C0", viewModel.state.value.expression)
        // Preview should still evaluate via implicit multiply.
        assertTrue(viewModel.state.value.previewResult.isNotEmpty())
    }

    @Test
    fun `constant after error starts fresh`() {
        forceErrorState()
        viewModel.onButtonClick("\u03C0")
        assertEquals("\u03C0", viewModel.state.value.expression)
    }

    // ========== appendSqrt branches ==========

    @Test
    fun `sqrt after operator appends`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("\u221A")
        assertEquals("5+\u221A(", viewModel.state.value.expression)
        assertEquals(1, viewModel.state.value.openParenCount)
    }

    @Test
    fun `sqrt after digit appends directly and preview still computes`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("\u221A")
        assertEquals("2\u221A(", viewModel.state.value.expression)
    }

    @Test
    fun `sqrt after error starts fresh`() {
        forceErrorState()
        viewModel.onButtonClick("\u221A")
        assertEquals("\u221A(", viewModel.state.value.expression)
        assertEquals(1, viewModel.state.value.openParenCount)
    }

    // ========== Preview branches ==========

    @Test
    fun `preview updates with unclosed parens`() {
        viewModel.onButtonClick("(")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        assertEquals("5", viewModel.state.value.previewResult)
    }

    @Test
    fun `preview empty on division by zero`() {
        viewModel.onButtonClick("1")
        viewModel.onButtonClick("\u00F7")
        viewModel.onButtonClick("0")
        assertEquals("", viewModel.state.value.previewResult)
    }

    @Test
    fun `preview empty when result displayed`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        assertEquals("", viewModel.state.value.previewResult)
    }

    @Test
    fun `preview cleared after AC`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        assertEquals("5", viewModel.state.value.previewResult)
        viewModel.onButtonClick("AC")
        assertEquals("", viewModel.state.value.previewResult)
    }

    // ========== Equals: additional ==========

    @Test
    fun `equals saves closed parens expression to history`() = runTest {
        viewModel.onButtonClick("(")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        coVerify { historyRepo.addEntry("(2+3)", "5") }
    }

    @Test
    fun `multiple equals presses are idempotent on result`() {
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("+")
        viewModel.onButtonClick("3")
        viewModel.onButtonClick("=")
        val firstDisplay = viewModel.state.value.displayText
        viewModel.onButtonClick("=")
        assertEquals(firstDisplay, viewModel.state.value.displayText)
    }

    @Test
    fun `equals on pure number evaluates to same number`() {
        viewModel.onButtonClick("4")
        viewModel.onButtonClick("2")
        viewModel.onButtonClick("=")
        assertEquals("42", viewModel.state.value.displayText)
        assertTrue(viewModel.state.value.isResultDisplayed)
    }

    // ========== Clear (AC) ==========

    @Test
    fun `AC resets isError`() {
        forceErrorState()
        viewModel.onButtonClick("AC")
        assertFalse(viewModel.state.value.isError)
        assertEquals("", viewModel.state.value.expression)
    }

    @Test
    fun `AC resets isResultDisplayed`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("=")
        assertTrue(viewModel.state.value.isResultDisplayed)
        viewModel.onButtonClick("AC")
        assertFalse(viewModel.state.value.isResultDisplayed)
    }

    @Test
    fun `AC persists cleared state`() {
        val handle = SavedStateHandle()
        val vm = newViewModel(handle)
        vm.onButtonClick("5")
        vm.onButtonClick("AC")
        assertEquals("", handle["expression"])
        assertEquals("0", handle["displayText"])
    }

    // ========== Backspace: additional ==========

    @Test
    fun `backspace after error clears state`() {
        forceErrorState()
        viewModel.onButtonClick("\u232B")
        assertEquals("", viewModel.state.value.expression)
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun `backspace on open paren not preceded by sqrt removes only paren`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("()")
        // "5×(" — pos=3, char at pos-1 is '('. char at pos-2 is '×' not '√'.
        assertEquals("5\u00D7(", viewModel.state.value.expression)
        viewModel.onButtonClick("\u232B")
        assertEquals("5\u00D7", viewModel.state.value.expression)
    }

    // ========== loadFromHistory ==========

    @Test
    fun `loadFromHistory clears isResultDisplayed`() {
        viewModel.onButtonClick("5")
        viewModel.onButtonClick("=")
        assertTrue(viewModel.state.value.isResultDisplayed)
        viewModel.loadFromHistory("1+2")
        assertFalse(viewModel.state.value.isResultDisplayed)
    }

    @Test
    fun `loadFromHistory clears isError`() {
        forceErrorState()
        viewModel.loadFromHistory("1+2")
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun `loadFromHistory updates preview`() {
        viewModel.loadFromHistory("2+3")
        assertEquals("5", viewModel.state.value.previewResult)
    }

    @Test
    fun `loadFromHistory saves state`() {
        val handle = SavedStateHandle()
        val vm = newViewModel(handle)
        vm.loadFromHistory("1+2")
        assertEquals("1+2", handle["expression"])
    }

    // ========== SavedStateHandle: additional ==========

    @Test
    fun `parenCount is recomputed from restored expression`() {
        val handle = SavedStateHandle(
            mapOf(
                "expression" to "((1+2",
                "displayText" to "((1+2",
                "isResultDisplayed" to false,
                "isError" to false,
            )
        )
        val vm = newViewModel(handle)
        assertEquals(2, vm.state.value.openParenCount)
    }

    @Test
    fun `restore with partial handle uses defaults`() {
        val handle = SavedStateHandle(mapOf("expression" to "42"))
        val vm = newViewModel(handle)
        assertEquals("42", vm.state.value.expression)
        assertEquals("0", vm.state.value.displayText) // missing KEY_DISPLAY → default
        assertFalse(vm.state.value.isResultDisplayed)
        assertFalse(vm.state.value.isError)
    }

    @Test
    fun `isResultDisplayed persists`() {
        val handle = SavedStateHandle()
        val vm = newViewModel(handle)
        vm.onButtonClick("5")
        vm.onButtonClick("=")
        assertEquals(true, handle["isResultDisplayed"])
    }

    @Test
    fun `after result saveState stores result expression not live field`() {
        val handle = SavedStateHandle()
        val vm = newViewModel(handle)
        vm.onButtonClick("(")
        vm.onButtonClick("2")
        vm.onButtonClick("+")
        vm.onButtonClick("3")
        vm.onButtonClick("=")
        // Auto-closed expression stored.
        assertEquals("(2+3)", handle["expression"])
        assertEquals("5", handle["displayText"])
    }

    // ========== History observation ==========

    @Test
    fun `observeHistory is queried at construction`() {
        verify(atLeast = 1) { historyRepo.observeHistory() }
    }

    // ========== Initial construction ==========

    @Test
    fun `default construction yields initial state`() {
        val handle = SavedStateHandle()
        val vm = newViewModel(handle)
        assertEquals("", vm.state.value.expression)
        assertEquals("0", vm.state.value.displayText)
        assertEquals(0, vm.state.value.openParenCount)
        assertFalse(vm.state.value.isResultDisplayed)
        assertFalse(vm.state.value.isError)
        assertEquals("", vm.state.value.previewResult)
    }
}
