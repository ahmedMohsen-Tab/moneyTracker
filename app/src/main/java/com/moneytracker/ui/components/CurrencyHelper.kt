package com.moneytracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.moneytracker.R
import com.moneytracker.domain.format.MoneyFormatter

/** Back-compat thin wrapper. New code should call [MoneyFormatter] directly. */
fun currencySymbol(currency: String): String = MoneyFormatter.symbol(currency)

/** Back-compat thin wrapper. New code should call [MoneyFormatter.format] directly. */
fun formatCurrency(amount: Double, currency: String): String =
    MoneyFormatter.format(amount, currency)

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
