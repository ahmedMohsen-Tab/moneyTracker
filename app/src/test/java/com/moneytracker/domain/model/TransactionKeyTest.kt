package com.moneytracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Regression test for the dashboard crash
 *
 *     java.lang.IllegalArgumentException: Key "1" was already used. If you are
 *     using LazyColumn/Row please make sure you provide a unique key for each item.
 *
 * Root cause: `expenses` and `incomes` are two separate Room tables, each with
 * its own `autoGenerate` id sequence, so an expense and an income are allowed
 * to share the same numeric id. `GetDashboardSummaryUseCase` merges both into
 * a single `recentTransactions` list; using `id` directly as the Compose
 * `LazyColumn` key collided and crashed the dashboard on first render after
 * the very first income + expense were inserted.
 *
 * Fix: [Transaction.stableKey] prefixes the id with the concrete subtype, so
 * keys are globally unique across the merged list.
 */
class TransactionKeyTest {

    private val today = LocalDate.now()
    private val noon = LocalTime.NOON

    @Test
    fun `expense and income with the same numeric id produce different keys`() {
        val expense = Transaction.ExpenseTransaction(
            id = 1,
            amount = 10.0,
            description = "coffee",
            date = today,
            time = noon,
            timestamp = 1L,
            category = Category.default,
            wallet = "Cash"
        )
        val income = Transaction.IncomeTransaction(
            id = 1,
            amount = 200.0,
            description = "salary",
            date = today,
            time = noon,
            timestamp = 2L,
            wallet = "Bank"
        )

        assertNotEquals(expense.stableKey(), income.stableKey())
    }

    @Test
    fun `keys for a merged expense+income list are all distinct`() {
        val transactions = listOf(
            Transaction.ExpenseTransaction(
                id = 1, amount = 10.0, description = "coffee",
                date = today, time = noon, timestamp = 1L,
                category = Category.default, wallet = "Cash"
            ),
            Transaction.IncomeTransaction(
                id = 1, amount = 200.0, description = "salary",
                date = today, time = noon, timestamp = 2L,
                wallet = "Bank"
            ),
            Transaction.ExpenseTransaction(
                id = 2, amount = 5.0, description = "taxi",
                date = today, time = noon, timestamp = 3L,
                category = Category.default, wallet = "Cash"
            )
        )

        val keys = transactions.map { it.stableKey() }
        assertEquals(keys.size, keys.distinct().size)
    }

    @Test
    fun `stableKey is deterministic for the same transaction`() {
        val tx = Transaction.IncomeTransaction(
            id = 42,
            amount = 1.0,
            description = "tip",
            date = today,
            time = noon,
            timestamp = 0L,
            wallet = "Cash"
        )
        assertEquals(tx.stableKey(), tx.stableKey())
    }
}

