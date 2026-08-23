package com.moneytracker.data.repository

import android.content.Context
import android.net.Uri
import com.moneytracker.data.local.dao.BudgetDao
import com.moneytracker.data.local.dao.CategoryBudgetDao
import com.moneytracker.data.local.dao.ExpenseDao
import com.moneytracker.data.local.dao.IncomeDao
import com.moneytracker.data.local.entity.ExpenseEntity
import com.moneytracker.data.local.entity.IncomeEntity
import com.moneytracker.data.local.preferences.UserPreferences
import com.moneytracker.data.mapper.toEntity
import com.moneytracker.domain.model.Budget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val userPreferences: UserPreferences,
    private val expenseDao: ExpenseDao,
    private val incomeDao: IncomeDao,
    private val budgetDao: BudgetDao,
    private val categoryBudgetDao: CategoryBudgetDao,
    @ApplicationContext private val context: Context
) {

    val currency = userPreferences.currency
    val theme = userPreferences.theme
    val language = userPreferences.language
    val dailySummaryEnabled = userPreferences.dailySummaryEnabled

    suspend fun setCurrency(currency: String) = userPreferences.setCurrency(currency)
    suspend fun setTheme(theme: String) = userPreferences.setTheme(theme)
    suspend fun setLanguage(tag: String) = userPreferences.setLanguage(tag)
    suspend fun setDailySummaryEnabled(enabled: Boolean) = userPreferences.setDailySummaryEnabled(enabled)

    suspend fun exportToCsv(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.appendLine("type,id,amount,categoryId,description,date,time,wallet")
                expenseDao.getAll().first().forEach {
                    writer.appendLine(
                        "expense,${it.expense.id},${it.expense.amount},${it.expense.categoryId},\"${it.expense.description}\",${it.expense.date},${it.expense.time},${it.expense.wallet}"
                    )
                }
                incomeDao.getAll().first().forEach {
                    writer.appendLine(
                        "income,${it.id},${it.amount},,\"${it.description}\",${it.date},${it.time},"
                    )
                }
            }
        }
    }

    suspend fun importFromCsv(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val lines = reader.readLines()
                if (lines.isEmpty()) return@withContext
                lines.drop(1).forEach { line ->
                    val parts = parseCsvLine(line)
                    if (parts.isEmpty()) return@forEach
                    when (parts[0]) {
                        "expense" -> {
                            if (parts.size >= 8) {
                                expenseDao.insert(
                                    ExpenseEntity(
                                        id = 0,
                                        amount = parts[2].toDoubleOrNull() ?: 0.0,
                                        categoryId = parts[3].toIntOrNull() ?: 10,
                                        description = parts.getOrNull(4) ?: "",
                                        date = parts.getOrNull(5) ?: "",
                                        time = parts.getOrNull(6) ?: "",
                                        timestamp = System.currentTimeMillis(),
                                        wallet = parts.getOrNull(7) ?: "Cash"
                                    )
                                )
                            }
                        }
                        "income" -> {
                            if (parts.size >= 7) {
                                incomeDao.insert(
                                    IncomeEntity(
                                        id = 0,
                                        amount = parts[2].toDoubleOrNull() ?: 0.0,
                                        description = parts.getOrNull(4) ?: "",
                                        date = parts.getOrNull(5) ?: "",
                                        time = parts.getOrNull(6) ?: "",
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun resetAll() {
        budgetDao.deleteAll()
        budgetDao.insert(Budget().toEntity())
        categoryBudgetDao.deleteAll()
        expenseDao.deleteAll()
        incomeDao.deleteAll()
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        line.forEach { char ->
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }
}
