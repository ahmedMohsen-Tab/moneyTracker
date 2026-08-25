package com.moneytracker.domain.model

import java.time.LocalDate

data class Rent(
    val amount: Double,
    val dueDate: LocalDate,
    val isPaid: Boolean = false
)

data class Debt(
    val id: Long = 0,
    val friendName: String,
    val amount: Double,
    val note: String,
    val date: LocalDate,
    val isSettled: Boolean = false
)

data class Credit(
    val id: Long = 0,
    val friendName: String,
    val amount: Double,
    val note: String,
    val date: LocalDate,
    val reminded: Boolean = false
)
