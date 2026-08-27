package com.moneytracker.domain.usecase

import com.moneytracker.domain.model.Category
import com.moneytracker.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Locks in the fix for the unsafe cast in DashboardViewModel — every transaction
 * type is exercised against every wallet, and the totals are verified directly.
 */
class CalculateWalletBalancesUseCaseTest {

    private val useCase = CalculateWalletBalancesUseCase()

    @Test
    fun `empty list returns all zeros`() {
        val balances = useCase(emptyList())
        assertEquals(0.0, balances.total, 0.0)
        assertEquals(0.0, balances.cash, 0.0)
        assertEquals(0.0, balances.bank, 0.0)
    }

    @Test
    fun `expenses subtract from the wallet and income adds`() {
        val today = LocalDate.now()
        val now = LocalTime.NOON
        val transactions = listOf(
            expense(amount = 100.0, wallet = "Cash", date = today, time = now),
            expense(amount = 50.0, wallet = "Bank", date = today, time = now),
            income(amount = 200.0, wallet = "Bank", date = today, time = now)
        )

        val balances = useCase(transactions)

        // Cash: -100 (only expense). Bank: -50 + 200 = +150. Total = -100 + 150 = 50.
        assertEquals(50.0, balances.total, 0.0)
        assertEquals(-100.0, balances.cash, 0.0)
        assertEquals(150.0, balances.bank, 0.0)
    }

    @Test
    fun `unknown wallet names fall back to cash without crashing`() {
        val today = LocalDate.now()
        val transactions = listOf(
            expense(amount = 25.0, wallet = "PayPal", date = today, time = LocalTime.NOON)
        )

        val balances = useCase(transactions)

        assertEquals(-25.0, balances.cash, 0.0)
        assertEquals(0.0, balances.bank, 0.0)
    }

    @Test
    fun `mixed expenses and incomes across both wallets`() {
        val today = LocalDate.now()
        val now = LocalTime.NOON
        val transactions = listOf(
            expense(10.0, "Cash", today, now),
            expense(20.0, "Bank", today, now),
            income(100.0, "Cash", today, now),
            income(200.0, "Bank", today, now),
        )

        val balances = useCase(transactions)

        assertEquals(90.0 + 180.0, balances.total, 0.0)
        assertEquals(90.0, balances.cash, 0.0)
        assertEquals(180.0, balances.bank, 0.0)
    }

    private fun expense(amount: Double, wallet: String, date: LocalDate, time: LocalTime) =
        Transaction.ExpenseTransaction(
            id = 1,
            amount = amount,
            description = "",
            date = date,
            time = time,
            timestamp = 0L,
            category = Category.default,
            wallet = wallet
        )

    private fun income(amount: Double, wallet: String, date: LocalDate, time: LocalTime) =
        Transaction.IncomeTransaction(
            id = 1,
            amount = amount,
            description = "",
            date = date,
            time = time,
            timestamp = 0L,
            wallet = wallet
        )
}
