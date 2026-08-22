package com.moneytracker.data.mapper

import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.ExpenseWithCategory
import com.moneytracker.data.local.entity.IncomeEntity
import com.moneytracker.domain.model.Budget
import com.moneytracker.domain.model.Category
import com.moneytracker.domain.model.Expense
import com.moneytracker.domain.model.Income
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

fun CategoryEntity.toCategory(): Category = Category(
    id = id,
    name = name,
    iconName = iconName,
    color = color
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    iconName = iconName,
    color = color
)

fun ExpenseWithCategory.toExpense(): Expense = Expense(
    id = expense.id,
    amount = expense.amount,
    category = category.toCategory(),
    description = expense.description,
    date = LocalDate.parse(expense.date),
    time = LocalTime.parse(expense.time),
    wallet = expense.wallet
)

fun Expense.toEntity(): com.moneytracker.data.local.entity.ExpenseEntity =
    com.moneytracker.data.local.entity.ExpenseEntity(
        id = id,
        amount = amount,
        categoryId = category.id,
        description = description,
        date = date.toString(),
        time = time.toString(),
        timestamp = LocalDateTime.of(date, time).toEpochSecond(ZoneOffset.UTC),
        wallet = wallet
    )

fun IncomeEntity.toIncome(): Income = Income(
    id = id,
    amount = amount,
    description = description,
    date = LocalDate.parse(date),
    time = LocalTime.parse(time),
    wallet = wallet
)

fun Income.toEntity(): IncomeEntity = IncomeEntity(
    id = id,
    amount = amount,
    description = description,
    date = date.toString(),
    time = time.toString(),
    timestamp = LocalDateTime.of(date, time).toEpochSecond(ZoneOffset.UTC),
    wallet = wallet
)

fun com.moneytracker.data.local.entity.BudgetEntity.toBudget(): Budget = Budget(
    monthlyBudget = monthlyBudget,
    dailyBudget = dailyBudget,
    currency = currency
)

fun Budget.toEntity(): com.moneytracker.data.local.entity.BudgetEntity =
    com.moneytracker.data.local.entity.BudgetEntity(
        monthlyBudget = monthlyBudget,
        dailyBudget = dailyBudget,
        currency = currency
    )
