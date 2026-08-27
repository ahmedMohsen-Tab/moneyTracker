package com.moneytracker.domain.model

/**
 * UI theme preference persisted in DataStore.
 * Replaces raw "Light"/"Dark"/"System" strings.
 */
enum class ThemeMode {
    LIGHT, DARK, SYSTEM;

    companion object {
        fun fromName(value: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}
