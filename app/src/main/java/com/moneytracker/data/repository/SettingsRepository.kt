package com.moneytracker.data.repository

import android.net.Uri
import com.moneytracker.data.local.dao.BudgetDao
import com.moneytracker.data.local.dao.CategoryBudgetDao
import com.moneytracker.data.local.dao.ExpenseDao
import com.moneytracker.data.local.dao.IncomeDao
import com.moneytracker.data.local.preferences.UserPreferences
import com.moneytracker.data.mapper.toEntity
import com.moneytracker.domain.model.Budget
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User preferences + destructive data operations only.
 * CSV backup/restore now lives in [BackupRepository].
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val userPreferences: UserPreferences,
    private val expenseDao: ExpenseDao,
    private val incomeDao: IncomeDao,
    private val budgetDao: BudgetDao,
    private val categoryBudgetDao: CategoryBudgetDao,
    private val backupRepository: BackupRepository
) {

    val currency = userPreferences.currency
    val theme = userPreferences.theme
    val language = userPreferences.language
    val dailySummaryEnabled = userPreferences.dailySummaryEnabled

    suspend fun setCurrency(currency: String) = userPreferences.setCurrency(currency)
    suspend fun setTheme(theme: String) = userPreferences.setTheme(theme)
    suspend fun setLanguage(tag: String) = userPreferences.setLanguage(tag)
    suspend fun setDailySummaryEnabled(enabled: Boolean) = userPreferences.setDailySummaryEnabled(enabled)

    suspend fun exportToCsv(uri: Uri) = backupRepository.exportToCsv(uri)
    suspend fun importFromCsv(uri: Uri) = backupRepository.importFromCsv(uri)

    suspend fun resetAll() {
        budgetDao.deleteAll()
        budgetDao.insert(Budget().toEntity())
        categoryBudgetDao.deleteAll()
        expenseDao.deleteAll()
        incomeDao.deleteAll()
    }
}
