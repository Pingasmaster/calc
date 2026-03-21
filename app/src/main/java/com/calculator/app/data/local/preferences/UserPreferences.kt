package com.calculator.app.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "calculator_prefs")

class UserPreferences(private val context: Context) {
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")

    val dynamicColorEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[DYNAMIC_COLOR_KEY] ?: false }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }
}
