/**
 * Room entity backing per-category monthly spending limits.
 *
 * The primary key is the category id, which means each category has at most
 * one budget. Inserting a new row for an existing category overwrites it
 * (`OnConflictStrategy.REPLACE` on the DAO).
 *
 * Storing `currency` per-budget is intentional: a user who set a "Coffee"
 * budget in USD keeps that limit even if they later change the global
 * default currency to EUR.
 */
package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey
    val categoryId: Int,
    val monthlyLimit: Double,
    val currency: String
)