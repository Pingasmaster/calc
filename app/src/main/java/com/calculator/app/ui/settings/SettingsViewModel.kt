package com.calculator.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.calculator.app.CalculatorApplication
import com.calculator.app.data.local.preferences.ThemeMode
import com.calculator.app.data.local.preferences.UserPreferences
import kotlinx.coroutines.launch

/**
 * Owns DataStore writes for theme settings. Writes are launched in
 * [viewModelScope] so they survive composition tear-down (e.g. when the
 * settings sheet is dismissed immediately after a toggle).
 */
class SettingsViewModel(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDynamicColor(enabled) }
    }

    fun setOledBlack(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setOledBlack(enabled) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CalculatorApplication
                SettingsViewModel(app.userPreferences)
            }
        }
    }
}
