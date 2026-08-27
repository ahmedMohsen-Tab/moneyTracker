/**
 * Domain models for per-category monthly budgets and their computed
 * usage against the current month.
 *
 * [CategoryBudgetUsage.percent] is a derived property: `spent / limit`
 * (returning 0 when `limit <= 0` so callers don't have to guard against
 * division-by-zero).
 *
 * [CategoryBudgetStatus] thresholds (60% / 90% / 100%) drive the colour
 * of the per-category progress bar on the dashboard.
 */
package com.moneytracker.domain.model

data class CategoryBudget(
    val categoryId: Int,
    val monthlyLimit: Double,
    val currency: String = "USD"
)

enum class CategoryBudgetStatus {
    OK, WARNING, NEAR_LIMIT, OVER_LIMIT
}

data class CategoryBudgetUsage(
    val category: Category,
    val limit: Double,
    val spent: Double,
    val currency: String,
    val status: CategoryBudgetStatus
) {
    val percent: Double get() = if (limit > 0) spent / limit else 0.0
}