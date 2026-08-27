package com.moneytracker.domain.format

/**
 * Pure money / currency formatting used across all layers (UI, notifications,
 * CSV exports, etc.) so a notification worker doesn't have to depend on a
 * Compose helper.
 */
object MoneyFormatter {

    fun symbol(currency: String): String = when (currency) {
        "USD" -> "$"
        "EUR" -> "€"
        "EGP" -> "E£"
        "SAR" -> "SAR "
        "AED" -> "AED "
        else -> "$"
    }

    fun format(amount: Double, currency: String): String =
        "${symbol(currency)}%.2f".format(amount)
}
