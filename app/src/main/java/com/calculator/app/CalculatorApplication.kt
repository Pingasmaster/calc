package com.calculator.app

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.StrictMode
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

    private companion object {
        const val TAG = "CalculatorApp"

        // Cap the per-launch dump to the 5 most recent non-self exits; older
        // entries are still in the platform buffer if we ever need to reach
        // further (e.g. via adb dumpsys).

        const val MAX_EXIT_REASONS = 5
    }

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
        if (isDebuggable()) {
            installStrictMode()
        }
        logPreviousExitReasons()
        userPreferences = UserPreferences(this)
        themeSettings = userPreferences.themeSettings
            .stateIn(appScope, SharingStarted.Eagerly, initialValue = null)
    }

    /**
     * Walks ActivityManager.getHistoricalProcessExitReasons() on every launch
     * and logs any non-EXIT_SELF entry (REASON_ANR, REASON_CRASH_NATIVE,
     * REASON_LOW_MEMORY, REASON_OTHER, ...). Captures ANRs / native crashes
     * that the standard uncaughtExceptionHandler never sees — equivalent to
     * having a tombstone for non-fatal exits. minSdk=35 means the API is
     * always available.
     */
    private fun logPreviousExitReasons() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val reasons = try {
            am.getHistoricalProcessExitReasons(packageName, android.os.Process.myPid(), MAX_EXIT_REASONS)
        } catch (se: SecurityException) {
            Log.w(TAG, "getHistoricalProcessExitReasons denied", se)
            return
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "getHistoricalProcessExitReasons invalid args", e)
            return
        }
        for (info in reasons) {
            if (info.reason == ApplicationExitInfo.REASON_EXIT_SELF) continue
            Log.w(
                TAG,
                "previous exit: reason=${info.reason} importance=${info.importance} " +
                    "pss=${info.pss} rss=${info.rss} timestamp=${info.timestamp} " +
                    "description=${info.description}",
            )
        }
    }

    /**
     * Flags debuggable APKs with policies that surface regressions before they
     * ship: disk I/O / network / slow calls on the UI thread, leaked closeables,
     * leaked registrations, leaked Activities. Stack traces land in logcat so
     * devs can fix them in the source instead of seeing them as Play-Console
     * ANRs in production. Flag check is used (not BuildConfig.DEBUG) because
     * the module disables `buildConfig` generation entirely.
     */
    private fun installStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build(),
        )
    }

    private fun isDebuggable(): Boolean = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

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

            // The remaining TRIM_MEMORY_* constants (COMPLETE, MODERATE,
            // RUNNING_*) are intentionally no-ops: the OS handles process
            // lifecycle for us at those levels, and our Room + DataStore
            // caches keep enough on disk to be rehydrated cheaply. Lint
            // wants an explicit `else` because the when is on an `@IntDef`
            // type (SwitchIntDef); fall through it here.
            else -> Unit
        }
    }
}
