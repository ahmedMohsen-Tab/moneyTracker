/**
 * Computes the dashboard's "Category Budgets" list: for each row in
 * `category_budgets`, sums the matching month's expenses and pairs it
 * with a [com.moneytracker.domain.model.CategoryBudgetStatus] threshold.
 *
 * Subscribes to **month-scoped** expenses only (not the full history) so
 * it stays cheap even on installs with years of data. The combine of three
 * flows (expenses / categories / category_budgets) is the source of
 * reactive updates: adding a new expense re-emits, editing a category
 * budget re-emits, etc.
 */
package com.moneytracker.domain.usecase

import com.moneytracker.data.repository.CategoryBudgetRepository
import com.moneytracker.data.repository.CategoryRepository
import com.moneytracker.data.repository.ExpenseRepository
import com.moneytracker.domain.model.Category
import com.moneytracker.domain.model.CategoryBudgetStatus
import com.moneytracker.domain.model.CategoryBudgetUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetCategoryBudgetUsageUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val categoryBudgetRepository: CategoryBudgetRepository
) {
    operator fun invoke(month: String): Flow<List<CategoryBudgetUsage>> = combine(
        expenseRepository.getExpensesByMonthString(month),
        categoryRepository.getAllCategories(),
        categoryBudgetRepository.getAll()
    ) { expenses, categories, budgets ->
        budgets.map { budget ->
            val cat = categories.find { it.id == budget.categoryId }
                ?: Category.default
            val spent = expenses.filter { it.category.id == budget.categoryId }
                .sumOf { it.amount }
            val status = when {
                budget.monthlyLimit <= 0 -> CategoryBudgetStatus.OK
                spent >= budget.monthlyLimit -> CategoryBudgetStatus.OVER_LIMIT
                spent >= budget.monthlyLimit * 0.9 -> CategoryBudgetStatus.NEAR_LIMIT
                spent >= budget.monthlyLimit * 0.6 -> CategoryBudgetStatus.WARNING
                else -> CategoryBudgetStatus.OK
            }
            CategoryBudgetUsage(
                category = cat,
                limit = budget.monthlyLimit,
                spent = spent,
                currency = budget.currency,
                status = status
            )
        }.sortedByDescending { it.percent }
    }
}