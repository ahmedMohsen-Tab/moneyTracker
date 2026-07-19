package com.moneytracker.ui.components

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

fun greeting(): String {
    val hour = java.time.LocalDateTime.now().hour
    return when (hour) {
        in 5..11 -> "Good Morning 👋"
        in 12..17 -> "Good Afternoon 👋"
        else -> "Good Evening 👋"
    }
}
