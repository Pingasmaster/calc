package com.calculator.app

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log
import androidx.room.Room
import com.calculator.app.data.local.db.CalculatorDatabase
import com.calculator.app.data.local.preferences.ThemeSettings
import com.calculator.app.data.local.preferences.UserPreferences
import kotlinx.coroutines.CoroutineExceptionHandler
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

    // SupervisorJob keeps sibling coroutines alive on failure; the exception
    // handler keeps the failure observable instead of being silently dropped
    // (DataStore IOException, for example, would otherwise just print to stderr).
    private val appScope = CoroutineScope(
        SupervisorJob() + CoroutineExceptionHandler { _, t ->
            Log.e("CalculatorApp", "Unhandled appScope failure", t)
        },
    )

    override fun onCreate() {
        super.onCreate()
        userPreferences = UserPreferences(this)
        themeSettings = userPreferences.themeSettings
            .stateIn(appScope, SharingStarted.Eagerly, initialValue = null)
    }

    /**
     * Voluntarily release caches the OS can regenerate cheaply. Per the
     * Android 17 memory-efficiency guidance, focus on the two levels the OS
     * raises when the UI is no longer visible: TRIM_MEMORY_UI_HIDDEN and
     * TRIM_MEMORY_BACKGROUND. The Room database connection pool and
     * DataStore keep enough of their state on disk that we don't touch
     * them; the eagerly-shared themeSettings StateFlow is also held by
     * Compose, so the system reclaims its snapshot on its own. Override
     * exists as the canonical hook so future caches land in one place.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            -> Unit
        }
    }
}
