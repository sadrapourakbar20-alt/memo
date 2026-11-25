package com.sadra.memo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sadra.memo.domain.model.SemesterConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.time.LocalDate

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val semStartKey = longPreferencesKey("semester_start")
    private val semEndKey = longPreferencesKey("semester_end")
    private val semInvertKey = booleanPreferencesKey("semester_invert")
    private val pinKey = stringPreferencesKey("pin_hash")
    private val biometricKey = booleanPreferencesKey("biometric_enabled")

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        when (prefs[themeKey]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[themeKey] = mode.name }
    }

    val semesterConfig: Flow<SemesterConfig> = context.settingsDataStore.data.map { prefs ->
        SemesterConfig(
            start = prefs[semStartKey]?.let { LocalDate.ofEpochDay(it) },
            end = prefs[semEndKey]?.let { LocalDate.ofEpochDay(it) },
            invertParity = prefs[semInvertKey] ?: false
        )
    }

    suspend fun setSemesterStart(date: LocalDate?) {
        context.settingsDataStore.edit {
            if (date == null) it.remove(semStartKey) else it[semStartKey] = date.toEpochDay()
        }
    }

    suspend fun setSemesterEnd(date: LocalDate?) {
        context.settingsDataStore.edit {
            if (date == null) it.remove(semEndKey) else it[semEndKey] = date.toEpochDay()
        }
    }

    suspend fun setInvertParity(value: Boolean) {
        context.settingsDataStore.edit { it[semInvertKey] = value }
    }

    suspend fun setSemester(config: SemesterConfig) {
        context.settingsDataStore.edit {
            val s = config.start
            val e = config.end
            if (s == null) it.remove(semStartKey) else it[semStartKey] = s.toEpochDay()
            if (e == null) it.remove(semEndKey) else it[semEndKey] = e.toEpochDay()
            it[semInvertKey] = config.invertParity
        }
    }

    /** رشته خالی یعنی قفلی تنظیم نشده */
    val pinHash: Flow<String?> = context.settingsDataStore.data.map { prefs -> prefs[pinKey] ?: "" }

    private fun hash(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(("memo-lock:" + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun setPin(pin: String) {
        context.settingsDataStore.edit { it[pinKey] = hash(pin) }
    }

    suspend fun clearPin() {
        context.settingsDataStore.edit {
            it.remove(pinKey)
            it.remove(biometricKey)
        }
    }

    val biometricEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[biometricKey] ?: false }

    suspend fun setBiometric(value: Boolean) {
        context.settingsDataStore.edit { it[biometricKey] = value }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = context.settingsDataStore.data.first()[pinKey] ?: return true
        return stored == hash(pin)
    }
}
