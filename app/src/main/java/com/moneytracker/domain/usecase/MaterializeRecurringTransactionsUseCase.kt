/**
 * Materialises concrete rows for every recurring expense / income whose
 * next occurrence falls on or before today.
 *
 * Idempotent: called by [com.moneytracker.notifications.RecurringTransactionsWorker]
 * once per day, and again on every cold start (the scheduler uses KEEP
 * policy so duplicate enqueues are fine). The worker also catches up
 * rows spawned while the device was off.
 *
 * Each template owns a `recurrenceGroupId`; we look up the latest
 * concrete row in the group to compute the next due date so edits to
 * the schedule (paused / resumed / deleted) propagate correctly.
 */
package com.moneytracker.domain.usecase

import com.moneytracker.data.repository.ExpenseRepository
import com.moneytracker.data.repository.IncomeRepository
import com.moneytracker.domain.model.Expense
import com.moneytracker.domain.model.Income
import com.moneytracker.domain.model.RecurrenceRule
import java.time.LocalDate
import javax.inject.Inject

/**
 * Walks every transaction that has a [RecurrenceRule] and creates the next
 * occurrence for each one whose `nextOccurrence(after = latestDate)` has
 * already passed (i.e. is on or before [today]).
 *
 * Idempotent: calling it twice in the same day is a no-op the second time,
 * because the freshly-inserted row becomes the new "latest" and its
 * next occurrence is in the future.
 *
 * Runs as plain `suspend` (no Flow / no DB transactions) so the WorkManager
 * worker that wraps it stays simple and easy to test.
 */
class MaterializeRecurringTransactionsUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
) {

    /**
     * @return number of new transactions created.
     */
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): Int {
        var created = 0
        created += materializeExpenses(today)
        created += materializeIncomes(today)
        return created
    }

    private suspend fun materializeExpenses(today: LocalDate): Int {
        var created = 0
        // De-dupe by series id so two rows in the same series don't both fire
        // for the same day. The DAO returns "all recurring" so a series can
        // appear more than once if it has historical rows.
        //
        // Legacy rows created before `recurrenceGroupId` was auto-assigned will
        // have a null group id. Multiple unrelated series can collide on
        // `null`, so we additionally key on (rule, anchor date) — that pairs
        // historical rows of one series together even when they don't share a
        // groupId yet.
        val seenSeries = mutableSetOf<String>()

        for (template in expenseRepository.getRecurring()) {
            val rule = template.recurrenceRule ?: continue
            val seriesKey = template.recurrenceGroupId
                ?: "legacy:${rule.encode()}:${template.date}"
            if (!seenSeries.add(seriesKey)) continue

            // Find the most recent occurrence in the series. If we don't have
            // one yet (no groupId set), fall back to the template itself.
            val latest = template.recurrenceGroupId
                ?.let { expenseRepository.getLatestInSeries(it) ?: template }
                ?: template
            created += advanceExpenseUntilDue(latest, rule, today)
        }
        return created
    }

    private suspend fun materializeIncomes(today: LocalDate): Int {
        var created = 0
        val seenSeries = mutableSetOf<String>()
        for (template in incomeRepository.getRecurring()) {
            val rule = template.recurrenceRule ?: continue
            val seriesKey = template.recurrenceGroupId
                ?: "legacy:${rule.encode()}:${template.date}"
            if (!seenSeries.add(seriesKey)) continue
            val latest = template.recurrenceGroupId
                ?.let { incomeRepository.getLatestInSeries(it) ?: template }
                ?: template
            created += advanceIncomeUntilDue(latest, rule, today)
        }
        return created
    }

    /**
     * Walk [latest.date] forward one period at a time. Each step that lands on
     * or before [today] is materialised. The first step that lands after today
     * stops the loop.
     *
     * Capped at [MAX_OCCURRENCES_PER_RUN] so a forgotten monthly bill from 5
     * years ago can't flood the table in one shot when the user finally opens
     * the app — it'll catch up gradually over the next few runs.
     */
    private suspend fun advanceExpenseUntilDue(
        latest: Expense,
        rule: RecurrenceRule,
        today: LocalDate
    ): Int {
        var cursor = latest.date
        var created = 0
        while (!cursor.isAfter(today) && created < MAX_OCCURRENCES_PER_RUN) {
            cursor = rule.nextOccurrence(cursor)
            if (cursor.isAfter(today)) break
            expenseRepository.insertExpense(latest.copy(id = 0, date = cursor))
            created++
        }
        return created
    }

    private suspend fun advanceIncomeUntilDue(
        latest: Income,
        rule: RecurrenceRule,
        today: LocalDate
    ): Int {
        var cursor = latest.date
        var created = 0
        while (!cursor.isAfter(today) && created < MAX_OCCURRENCES_PER_RUN) {
            cursor = rule.nextOccurrence(cursor)
            if (cursor.isAfter(today)) break
            incomeRepository.insertIncome(latest.copy(id = 0, date = cursor))
            created++
        }
        return created
    }

    companion object {
        const val MAX_OCCURRENCES_PER_RUN = 50
    }
}
