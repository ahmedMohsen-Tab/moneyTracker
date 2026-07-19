package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val categoryId: Int,
    val description: String,
    val date: String,
    val time: String,
    val timestamp: Long,
    val wallet: String = "Cash"
)
