package com.moneytracker.di

import android.content.Context
import androidx.room.Room
import com.moneytracker.data.local.AppDatabase
import com.moneytracker.data.local.dao.BudgetDao
import com.moneytracker.data.local.dao.CategoryBudgetDao
import com.moneytracker.data.local.dao.CategoryDao
import com.moneytracker.data.local.dao.ExpenseDao
import com.moneytracker.data.local.dao.IncomeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE incomes ADD COLUMN wallet TEXT NOT NULL DEFAULT 'Cash'")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recurring transactions: optional rule + group id for both expenses and incomes
        db.execSQL("ALTER TABLE expenses ADD COLUMN recurrenceRule TEXT")
        db.execSQL("ALTER TABLE expenses ADD COLUMN recurrenceGroupId TEXT")
        db.execSQL("ALTER TABLE incomes ADD COLUMN recurrenceRule TEXT")
        db.execSQL("ALTER TABLE incomes ADD COLUMN recurrenceGroupId TEXT")
        // Per-category budgets
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS category_budgets (
                categoryId INTEGER NOT NULL PRIMARY KEY,
                monthlyLimit REAL NOT NULL,
                currency TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        callback: AppDatabase.Callback
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "money_tracker.db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .addCallback(callback)
            .build()
    }

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideIncomeDao(database: AppDatabase): IncomeDao = database.incomeDao()

    @Provides
    fun provideBudgetDao(database: AppDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideCategoryBudgetDao(database: AppDatabase): CategoryBudgetDao = database.categoryBudgetDao()
}
