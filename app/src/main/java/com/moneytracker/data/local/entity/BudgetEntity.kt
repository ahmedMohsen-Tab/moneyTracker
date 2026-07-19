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
