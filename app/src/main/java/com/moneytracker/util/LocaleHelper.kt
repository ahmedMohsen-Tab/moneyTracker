/**
 * Thin wrapper around `AppCompatDelegate.setApplicationLocales` for
 * switching the app's runtime locale.
 *
 * `apply(tag)` accepts our enum-style strings ("system", "en", "ar")
 * and translates "system" to an empty locale list (which the platform
 * treats as "follow the device"). Unknown tags are passed through.
 *
 * `currentTag()` reads the currently-applied locale back from
 * `AppCompatDelegate.getApplicationLocales()` and normalises it to the
 * same enum-style string so callers can do a stable equality check.
 *
 * Used by [com.moneytracker.MoneyTrackerApplication.onCreate] (initial
 * sync) and by the Settings screen (user-initiated change).
 */
package com.moneytracker.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {
    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val ARABIC = "ar"

    fun apply(tag: String) {
        val locales = if (tag == SYSTEM || tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun currentTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return SYSTEM
        val tag = locales.toLanguageTags()
        // Normalize common short forms back to our enum values for stable comparison.
        return when {
            tag.startsWith("ar") -> ARABIC
            tag.startsWith("en") -> ENGLISH
            tag.isBlank() -> SYSTEM
            else -> tag
        }
    }
}