package com.moneytracker.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Expense(
    val id: Long = 0,
    val amount: Double,
    val category: Category,
    val description: String,
    val date: LocalDate,
    val time: LocalTime,
    val wallet: String = "Cash"
)
