package com.moneytracker.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

sealed class Transaction {
    abstract val id: Long
    abstract val amount: Double
    abstract val description: String
    abstract val date: LocalDate
    abstract val time: LocalTime
    abstract val timestamp: Long

    data class ExpenseTransaction(
        override val id: Long,
        override val amount: Double,
        override val description: String,
        override val date: LocalDate,
        override val time: LocalTime,
        override val timestamp: Long,
        val category: Category,
        val wallet: String
    ) : Transaction()

    data class IncomeTransaction(
        override val id: Long,
        override val amount: Double,
        override val description: String,
        override val date: LocalDate,
        override val time: LocalTime,
        override val timestamp: Long,
        val wallet: String
    ) : Transaction()
}

fun Expense.toTransaction(): Transaction.ExpenseTransaction = Transaction.ExpenseTransaction(
    id = id,
    amount = amount,
    description = description,
    date = date,
    time = time,
    timestamp = LocalDateTime.of(date, time).toEpochSecond(ZoneOffset.UTC),
    category = category,
    wallet = wallet
)

fun Income.toTransaction(): Transaction.IncomeTransaction = Transaction.IncomeTransaction(
    id = id,
    amount = amount,
    description = description,
    date = date,
    time = time,
    timestamp = LocalDateTime.of(date, time).toEpochSecond(ZoneOffset.UTC),
    wallet = wallet
)

fun Transaction.displaySign(): String = when (this) {
    is Transaction.ExpenseTransaction -> "-"
    is Transaction.IncomeTransaction -> "+"
}

fun Transaction.isExpense(): Boolean = this is Transaction.ExpenseTransaction
fun Transaction.isIncome(): Boolean = this is Transaction.IncomeTransaction
