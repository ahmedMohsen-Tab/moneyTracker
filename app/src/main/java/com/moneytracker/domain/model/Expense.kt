/**
 * Domain model for a single expense — the UI-facing counterpart of
 * [com.moneytracker.data.local.entity.ExpenseEntity].
 *
 * `date` and `time` are split into separate fields (not a single
 * `Instant`) so the date-picker / time-picker UI controls can bind to
 * them directly.
 *
 * `recurrenceRule` is null for one-off expenses and a
 * [com.moneytracker.domain.model.RecurrenceRule] for recurring templates.
 * `recurrenceGroupId` ties together all concrete rows materialised from
 * the same template (see
 * [com.moneytracker.domain.usecase.MaterializeRecurringTransactionsUseCase]).
 */
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
    val wallet: String = "Cash",
    val recurrenceRule: RecurrenceRule? = null,
    val recurrenceGroupId: String? = null
)
