package app.unhook.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Currency
import java.util.Locale

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** ISO 4217 code used to show savings. */
    val currency: String = defaultCurrency(),
    val notifications: Boolean = true,
)

private fun defaultCurrency(): String =
    runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull() ?: "USD"

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map {
        Settings(
            theme = it[THEME]?.let { t -> runCatching { ThemeMode.valueOf(t) }.getOrNull() } ?: ThemeMode.SYSTEM,
            currency = it[CURRENCY] ?: defaultCurrency(),
            notifications = it[NOTIFICATIONS] ?: true,
        )
    }

    suspend fun now(): Settings = settings.first()

    suspend fun setTheme(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    /** Pins the locale's currency on first launch so a later language switch doesn't relabel savings. */
    suspend fun pinCurrency() {
        store.edit { if (it[CURRENCY] == null) it[CURRENCY] = defaultCurrency() }
    }

    suspend fun setCurrency(code: String) {
        store.edit { it[CURRENCY] = code }
    }

    suspend fun setNotifications(enabled: Boolean) {
        store.edit { it[NOTIFICATIONS] = enabled }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val CURRENCY = stringPreferencesKey("currency")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
    }
}
