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