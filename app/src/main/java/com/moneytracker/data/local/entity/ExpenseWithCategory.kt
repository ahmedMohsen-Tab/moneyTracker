/**
 * Room relation used by expense queries that need the joined-in `Category`.
 *
 * Marked `@Transaction` on the DAO methods that return it so the join is
 * atomic and the caller never sees a half-populated row.
 *
 * Performance note: the join is per-row, so prefer the bare
 * [ExpenseEntity] + a separate `CategoryDao.getById` when you only need
 * one row at a time (e.g. in `AddExpenseViewModel.loadExpense`).
 */
package com.moneytracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ExpenseWithCategory(
    @Embedded val expense: ExpenseEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity
)
