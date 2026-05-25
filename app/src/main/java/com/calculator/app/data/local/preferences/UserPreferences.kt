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
private val HAPTICS_ENABLED_KEY = booleanPreferencesKey("haptics_enabled")

private val Context.dataStore by preferencesDataStore(
    name = "calculator_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ThemeSettings(val themeMode: ThemeMode, val dynamicColor: Boolean, val oledBlack: Boolean, val hapticsEnabled: Boolean) {
    companion object {
        val Default = ThemeSettings(
            themeMode = ThemeMode.SYSTEM,
            dynamicColor = true,
            oledBlack = false,
            hapticsEnabled = true,
        )
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
                    hapticsEnabled = prefs[HAPTICS_ENABLED_KEY] ?: true,
                )
            }
            .distinctUntilChanged()

    val themeMode: Flow<ThemeMode> = themeSettings.map { it.themeMode }.distinctUntilChanged()
    val dynamicColorEnabled: Flow<Boolean> = themeSettings.map { it.dynamicColor }.distinctUntilChanged()
    val oledBlackEnabled: Flow<Boolean> = themeSettings.map { it.oledBlack }.distinctUntilChanged()
    val hapticsEnabled: Flow<Boolean> = themeSettings.map { it.hapticsEnabled }.distinctUntilChanged()

    /**
     * Composite setter: applies any non-null field in a single [DataStore.edit]
     * block so multi-field UI updates cost one disk flush instead of N.
     */
    suspend fun setThemeSettings(
        mode: ThemeMode? = null,
        dynamicColor: Boolean? = null,
        oledBlack: Boolean? = null,
        hapticsEnabled: Boolean? = null,
    ) {
        dataStore.edit { prefs ->
            if (mode != null) {
                prefs[THEME_MODE_KEY] = when (mode) {
                    ThemeMode.SYSTEM -> "system"
                    ThemeMode.LIGHT -> "light"
                    ThemeMode.DARK -> "dark"
                }
            }
            if (dynamicColor != null) prefs[DYNAMIC_COLOR_KEY] = dynamicColor
            if (oledBlack != null) prefs[OLED_BLACK_KEY] = oledBlack
            if (hapticsEnabled != null) prefs[HAPTICS_ENABLED_KEY] = hapticsEnabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) = setThemeSettings(mode = mode)
    suspend fun setDynamicColor(enabled: Boolean) = setThemeSettings(dynamicColor = enabled)
    suspend fun setOledBlack(enabled: Boolean) = setThemeSettings(oledBlack = enabled)
    suspend fun setHapticsEnabled(enabled: Boolean) = setThemeSettings(hapticsEnabled = enabled)
}
