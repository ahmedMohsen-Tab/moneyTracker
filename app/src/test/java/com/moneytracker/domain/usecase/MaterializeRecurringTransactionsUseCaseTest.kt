package com.moneytracker.domain.usecase

import com.moneytracker.data.repository.ExpenseRepository
import com.moneytracker.data.repository.IncomeRepository
import com.moneytracker.domain.model.Category
import com.moneytracker.domain.model.Expense
import com.moneytracker.domain.model.Income
import com.moneytracker.domain.model.RecurrenceRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class MaterializeRecurringTransactionsUseCaseTest {

    private val expenseRepo = mockk<ExpenseRepository>(relaxed = true)
    private val incomeRepo = mockk<IncomeRepository>(relaxed = true)
    private val useCase = MaterializeRecurringTransactionsUseCase(expenseRepo, incomeRepo)

    private val food = Category(id = 1, name = "Food", iconName = "Restaurant", color = 0)

    @Test
    fun `daily expense that is 5 days overdue inserts 5 occurrences`() = runTest {
        val startDate = LocalDate.of(2026, 8, 20)
        val today = LocalDate.of(2026, 8, 25)
        val template = expense(
            date = startDate,
            amount = 10.0,
            rule = RecurrenceRule.Daily,
            groupId = "rent"
        )
        coEvery { expenseRepo.getRecurring() } returns listOf(template)
        coEvery { expenseRepo.getLatestInSeries("rent") } returns template

        val created = useCase(today)

        assertEquals(5, created)
        // Each insert is on the expected day, walking forward from the template date.
        coVerifyOrder {
            expenseRepo.insertExpense(match { it.date == LocalDate.of(2026, 8, 21) })
            expenseRepo.insertExpense(match { it.date == LocalDate.of(2026, 8, 22) })
            expenseRepo.insertExpense(match { it.date == LocalDate.of(2026, 8, 23) })
            expenseRepo.insertExpense(match { it.date == LocalDate.of(2026, 8, 24) })
            expenseRepo.insertExpense(match { it.date == LocalDate.of(2026, 8, 25) })
        }
    }

    @Test
    fun `weekly expense on Monday materialises only upcoming Mondays up to today`() = runTest {
        // Template dated 2026-08-26 (Wed). Recurring every Friday → next is 2026-08-28.
        // Today 2026-09-04 (Fri). Fridays between: 8-28, 9-04. That's 2.
        val template = expense(
            date = LocalDate.of(2026, 8, 26),
            rule = RecurrenceRule.Weekly(DayOfWeek.FRIDAY),
            groupId = "friday-coffee"
        )
        coEvery { expenseRepo.getRecurring() } returns listOf(template)
        coEvery { expenseRepo.getLatestInSeries("friday-coffee") } returns template

        val created = useCase(LocalDate.of(2026, 9, 4))

        assertEquals(2, created)
    }

    @Test
    fun `monthly bill clamps day of month when target month is shorter`() = runTest {
        // 31st of every month. Today is 2026-09-15. Next occurrence from 2026-08-31
        // is 2026-09-30 (clamped). That's still in the future, so nothing fires.
        val template = expense(
            date = LocalDate.of(2026, 8, 31),
            amount = 100.0,
            rule = RecurrenceRule.Monthly(dayOfMonth = 31),
            groupId = "rent"
        )
        coEvery { expenseRepo.getRecurring() } returns listOf(template)
        coEvery { expenseRepo.getLatestInSeries("rent") } returns template

        val created = useCase(LocalDate.of(2026, 9, 15))

        assertEquals(0, created)
        coVerify(exactly = 0) { expenseRepo.insertExpense(any()) }
    }

    @Test
    fun `income with no due occurrences inserts nothing`() = runTest {
        val template = income(
            date = LocalDate.of(2026, 9, 30),
            amount = 1000.0,
            rule = RecurrenceRule.Monthly(dayOfMonth = 30),
            groupId = "salary"
        )
        coEvery { incomeRepo.getRecurring() } returns listOf(template)
        coEvery { incomeRepo.getLatestInSeries("salary") } returns template

        val created = useCase(LocalDate.of(2026, 8, 26))

        assertEquals(0, created)
    }

    @Test
    fun `multiple rows in the same series are coalesced into one materialization`() = runTest {
        // The DAO returns every recurring row; two of them share a groupId, so
        // only the latest should be used as the anchor for advancement.
        val older = expense(
            date = LocalDate.of(2026, 8, 1),
            rule = RecurrenceRule.Daily,
            groupId = "rent"
        )
        val newer = expense(
            date = LocalDate.of(2026, 8, 20),
            rule = RecurrenceRule.Daily,
            groupId = "rent"
        )
        coEvery { expenseRepo.getRecurring() } returns listOf(newer, older)
        // The DAO says the most recent in the series is "newer".
        coEvery { expenseRepo.getLatestInSeries("rent") } returns newer

        val created = useCase(LocalDate.of(2026, 8, 25))

        // Only 5 days from the newer anchor should fire, not 24 from "older".
        assertEquals(5, created)
        coVerify(exactly = 5) { expenseRepo.insertExpense(any()) }
    }

    @Test
    fun `rows without a recurrence rule are skipped`() = runTest {
        val template = expense(date = LocalDate.of(2026, 8, 20), rule = null, groupId = null)
        coEvery { expenseRepo.getRecurring() } returns listOf(template)

        val created = useCase(LocalDate.of(2026, 8, 25))

        assertEquals(0, created)
        coVerify(exactly = 0) { expenseRepo.insertExpense(any()) }
    }

    @Test
    fun `catches up at most MAX_OCCURRENCES_PER_RUN even if years are overdue`() = runTest {
        // Monthly bill from 2018-01-15, today is 2026-08-26. ~104 months overdue.
        // Should clamp to MAX_OCCURRENCES_PER_RUN = 50 per run.
        val template = expense(
            date = LocalDate.of(2018, 1, 15),
            rule = RecurrenceRule.Monthly(dayOfMonth = 15),
            groupId = "old-bill"
        )
        coEvery { expenseRepo.getRecurring() } returns listOf(template)
        coEvery { expenseRepo.getLatestInSeries("old-bill") } returns template

        val created = useCase(LocalDate.of(2026, 8, 26))

        assertEquals(MaterializeRecurringTransactionsUseCase.MAX_OCCURRENCES_PER_RUN, created)
    }

    private fun expense(
        date: LocalDate,
        amount: Double = 25.0,
        rule: RecurrenceRule?,
        groupId: String?
    ) = Expense(
        id = 1,
        amount = amount,
        category = food,
        description = "test",
        date = date,
        time = java.time.LocalTime.NOON,
        wallet = "Cash",
        recurrenceRule = rule,
        recurrenceGroupId = groupId
    )

    private fun income(
        date: LocalDate,
        amount: Double,
        rule: RecurrenceRule,
        groupId: String
    ) = Income(
        id = 1,
        amount = amount,
        description = "test",
        date = date,
        time = java.time.LocalTime.NOON,
        wallet = "Bank",
        recurrenceRule = rule,
        recurrenceGroupId = groupId
    )
}
