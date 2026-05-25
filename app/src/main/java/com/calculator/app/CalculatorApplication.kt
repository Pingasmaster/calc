package com.calculator.app

import android.app.Application
import androidx.room.Room
import com.calculator.app.data.local.db.CalculatorDatabase
import com.calculator.app.data.local.preferences.ThemeSettings
import com.calculator.app.data.local.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CalculatorApplication : Application() {

    val database: CalculatorDatabase by lazy {
        Room.databaseBuilder(
            this,
            CalculatorDatabase::class.java,
            "calculator.db",
        )
            .addMigrations(CalculatorDatabase.MIGRATION_1_2, CalculatorDatabase.MIGRATION_2_3)
            .build()
    }

    lateinit var userPreferences: UserPreferences
        private set

    /**
     * Hot, eagerly cached theme settings. Splash screen waits for the first
     * non-null value before handing off to Compose so the first composition
     * already has real prefs (no theme flash).
     */
    lateinit var themeSettings: StateFlow<ThemeSettings?>
        private set

    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        userPreferences = UserPreferences(this)
        themeSettings = userPreferences.themeSettings
            .stateIn(appScope, SharingStarted.Eagerly, initialValue = null)
    }
}
