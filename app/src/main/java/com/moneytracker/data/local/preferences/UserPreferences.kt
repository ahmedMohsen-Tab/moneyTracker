package com.moneytracker.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.dataStore

    val currency: Flow<String> = dataStore.data.map { preferences ->
        preferences[CURRENCY_KEY] ?: "USD"
    }

    val theme: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "System"
    }

    val language: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "system"
    }

    val dailySummaryEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DAILY_SUMMARY_KEY] ?: true
    }

    suspend fun setCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[CURRENCY_KEY] = currency
        }
    }

    suspend fun setTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun setLanguage(tag: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = tag
        }
    }

    suspend fun setDailySummaryEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DAILY_SUMMARY_KEY] = enabled
        }
    }

    companion object {
        private val CURRENCY_KEY = stringPreferencesKey("currency")
        private val THEME_KEY = stringPreferencesKey("theme")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val DAILY_SUMMARY_KEY = booleanPreferencesKey("daily_summary")
    }
}
