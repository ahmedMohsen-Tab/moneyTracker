/**
 * Repository wrapping [CategoryBudgetDao]. Surfaces the per-category
 * monthly limits as a `Flow<List<CategoryBudget>>` for the dashboard and
 * the category-budget usage use case.
 */
package com.moneytracker.data.repository

import com.moneytracker.data.local.dao.CategoryBudgetDao
import com.moneytracker.data.local.entity.CategoryBudgetEntity
import com.moneytracker.domain.model.CategoryBudget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryBudgetRepository @Inject constructor(
    private val dao: CategoryBudgetDao
) {
    fun getAll(): Flow<List<CategoryBudget>> =
        dao.getAll().map { list -> list.map { CategoryBudget(it.categoryId, it.monthlyLimit, it.currency) } }

    suspend fun upsert(budget: CategoryBudget) {
        dao.upsert(CategoryBudgetEntity(budget.categoryId, budget.monthlyLimit, budget.currency))
    }

    suspend fun delete(categoryId: Int) = dao.deleteByCategory(categoryId)

    suspend fun deleteAll() = dao.deleteAll()
}