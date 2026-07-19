package com.moneytracker.domain.model

data class Budget(
    val monthlyBudget: Double = 0.0,
    val dailyBudget: Double = 0.0,
    val currency: String = "USD"
)
