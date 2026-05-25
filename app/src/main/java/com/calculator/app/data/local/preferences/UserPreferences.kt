package com.calculator.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
private val OLED_BLACK_KEY = booleanPreferencesKey("oled_black")

private val Context.dataStore by preferencesDataStore(
    name = "calculator_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ThemeSettings(
    val themeMode: ThemeMode,
    val dynamicColor: Boolean,
    val oledBlack: Boolean,
) {
    companion object {
        val Default = ThemeSettings(ThemeMode.SYSTEM, dynamicColor = true, oledBlack = false)
    }
}

class UserPreferences(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.dataStore)

    val themeSettings: Flow<ThemeSettings> =
        dataStore.data
            .map { prefs ->
                ThemeSettings(
                    themeMode = when (prefs[THEME_MODE_KEY]) {
                        "light" -> ThemeMode.LIGHT
                        "dark" -> ThemeMode.DARK
                        else -> ThemeMode.SYSTEM
                    },
                    dynamicColor = prefs[DYNAMIC_COLOR_KEY] ?: true,
                    oledBlack = prefs[OLED_BLACK_KEY] ?: false,
                )
            }
            .distinctUntilChanged()

    val themeMode: Flow<ThemeMode> = themeSettings.map { it.themeMode }.distinctUntilChanged()
    val dynamicColorEnabled: Flow<Boolean> = themeSettings.map { it.dynamicColor }.distinctUntilChanged()
    val oledBlackEnabled: Flow<Boolean> = themeSettings.map { it.oledBlack }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit {
            it[THEME_MODE_KEY] = when (mode) {
                ThemeMode.SYSTEM -> "system"
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
            }
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }

    suspend fun setOledBlack(enabled: Boolean) {
        dataStore.edit { it[OLED_BLACK_KEY] = enabled }
    }
}
