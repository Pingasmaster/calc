package com.calculator.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserPreferencesTest {

    private lateinit var context: Context
    private lateinit var dataStoreFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFile = File(context.cacheDir, "test_prefs_${System.nanoTime()}.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { dataStoreFile }
    }

    @After
    fun tearDown() {
        scope.cancel()
        dataStoreFile.delete()
    }

    private fun prefs() = UserPreferences(dataStore)

    @Test
    fun `default themeMode is SYSTEM`() = runTest {
        assertEquals(ThemeMode.SYSTEM, prefs().themeMode.first())
    }

    @Test
    fun `default dynamicColorEnabled is true`() = runTest {
        assertTrue(prefs().dynamicColorEnabled.first())
    }

    @Test
    fun `default oledBlackEnabled is false`() = runTest {
        assertFalse(prefs().oledBlackEnabled.first())
    }

    @Test
    fun `setThemeMode LIGHT persists`() = runTest {
        val p = prefs()
        p.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, p.themeMode.first())
    }

    @Test
    fun `setThemeMode DARK persists`() = runTest {
        val p = prefs()
        p.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, p.themeMode.first())
    }

    @Test
    fun `setThemeMode SYSTEM persists`() = runTest {
        val p = prefs()
        p.setThemeMode(ThemeMode.DARK)
        p.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, p.themeMode.first())
    }

    @Test
    fun `setDynamicColor false persists`() = runTest {
        val p = prefs()
        p.setDynamicColor(false)
        assertFalse(p.dynamicColorEnabled.first())
    }

    @Test
    fun `setOledBlack true persists`() = runTest {
        val p = prefs()
        p.setOledBlack(true)
        assertTrue(p.oledBlackEnabled.first())
    }

    @Test
    fun `round trip all three preferences`() = runTest {
        val p = prefs()
        p.setThemeMode(ThemeMode.LIGHT)
        p.setDynamicColor(false)
        p.setOledBlack(true)
        assertEquals(ThemeMode.LIGHT, p.themeMode.first())
        assertFalse(p.dynamicColorEnabled.first())
        assertTrue(p.oledBlackEnabled.first())
    }

    @Test
    fun `unknown theme string falls back to SYSTEM`() = runTest {
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "garbage" }
        assertEquals(ThemeMode.SYSTEM, prefs().themeMode.first())
    }

    @Test
    fun `default hapticsEnabled is true`() = runTest {
        assertTrue(prefs().hapticsEnabled.first())
    }

    @Test
    fun `setHapticsEnabled false persists`() = runTest {
        val p = prefs()
        p.setHapticsEnabled(false)
        assertFalse(p.hapticsEnabled.first())
    }

    @Test
    fun `setHapticsEnabled true persists`() = runTest {
        val p = prefs()
        p.setHapticsEnabled(false)
        p.setHapticsEnabled(true)
        assertTrue(p.hapticsEnabled.first())
    }

    @Test
    fun `setThemeSettings composite applies all non-null fields in one edit`() = runTest {
        val p = prefs()
        p.setThemeSettings(
            mode = ThemeMode.DARK,
            dynamicColor = false,
            oledBlack = true,
            hapticsEnabled = false,
        )
        val settings = p.themeSettings.first()
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertFalse(settings.dynamicColor)
        assertTrue(settings.oledBlack)
        assertFalse(settings.hapticsEnabled)
    }

    @Test
    fun `setThemeSettings leaves null fields untouched`() = runTest {
        val p = prefs()
        p.setThemeMode(ThemeMode.LIGHT)
        p.setHapticsEnabled(false)
        // Composite write that only touches dynamicColor:
        p.setThemeSettings(dynamicColor = false)
        val settings = p.themeSettings.first()
        assertEquals(ThemeMode.LIGHT, settings.themeMode)
        assertFalse(settings.dynamicColor)
        assertFalse(settings.hapticsEnabled)
    }
}
