/**
 * Aggregated financial snapshot exposed to the UI.
 *
 * `totalBalance = cash + bank` (Credit Card was removed; legacy rows are
 * re-bucketed to CASH on import).
 *
 * `recentTransactions` is already merged + sorted by timestamp (most
 * recent first) and capped at 10 — the dashboard renders it directly.
 */
package com.moneytracker.domain.model

import java.time.LocalDate

data class Rent(
    val amount: Double,
    val dueDate: LocalDate,
    val isPaid: Boolean = false
)

data class Debt(
    val id: Long = 0,
    val friendName: String,
    val amount: Double,
    val note: String,
    val date: LocalDate,
    val isSettled: Boolean = false
)

data class Credit(
    val id: Long = 0,
    val friendName: String,
    val amount: Double,
    val note: String,
    val date: LocalDate,
    val reminded: Boolean = false
)
