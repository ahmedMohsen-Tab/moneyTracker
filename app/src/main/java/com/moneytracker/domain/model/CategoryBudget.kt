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