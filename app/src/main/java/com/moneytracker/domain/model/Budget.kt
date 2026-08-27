/**
 * Domain model for the user's current monthly / daily budget.
 *
 * `monthlyBudget` is the source of truth for the dashboard's "remaining"
 * and "budget usage" calculations; `dailyBudget` is a derived/legacy field
 * kept for the daily-summary notification (which is sent at 8 PM with
 * today's spending against an optional daily limit).
 */
package com.moneytracker.domain.model

data class Budget(
    val monthlyBudget: Double = 0.0,
    val dailyBudget: Double = 0.0,
    val currency: String = "USD"
)
