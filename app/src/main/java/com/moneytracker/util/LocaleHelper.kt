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