package com.moneytracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.moneytracker.R

fun currencySymbol(currency: String): String = when (currency) {
    "USD" -> "$"
    "EUR" -> "€"
    "EGP" -> "E£"
    "SAR" -> "﷼"
    "AED" -> "د.إ"
    else -> "$"
}

fun formatCurrency(amount: Double, currency: String): String {
    val symbol = currencySymbol(currency)
    return "$symbol%.2f".format(amount)
}

@Composable
fun greeting(): String {
    val hour = java.time.LocalDateTime.now().hour
    val resId = when (hour) {
        in 5..11 -> R.string.greeting_morning
        in 12..17 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    return stringResource(resId)
}
