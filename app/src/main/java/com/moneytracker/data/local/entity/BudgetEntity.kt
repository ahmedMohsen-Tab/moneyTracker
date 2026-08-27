/**
 * Room entity backing the [com.moneytracker.domain.model.Budget] domain model.
 *
 * There is exactly one row in this table at any time (the user's current
 * monthly/daily budget configuration). Insertion uses REPLACE conflict
 * resolution to keep it as a single-row table without needing a separate
 * "is_active" flag.
 *
 * `currency` is stored so the budget survives the user changing their
 * default currency later — we always render the budget in the currency the
 * user entered it in.
 */
package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey
    val id: Int = 1,
    val monthlyBudget: Double,
    val dailyBudget: Double,
    val currency: String
)
