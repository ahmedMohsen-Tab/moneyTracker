/**
 * The single Room database for the app.
 *
 * Holds expenses, incomes, the singleton budget row, the seeded category
 * list, and the per-category budgets. Schema version is 3; if you bump it,
 * add a `Migration` to [com.moneytracker.di.DatabaseModule] — do not rely
 * on `fallbackToDestructiveMigration`.
 *
 * The `Callback` runs once on first creation of the DB on a device and:
 *  - seeds the canonical category list (see [seedCategories]); and
 *  - seeds an empty budget row (see [seedBudget]).
 *
 * Seeding happens on the IO dispatcher via the application's application
 * scope, never on the main thread.
 */
package com.moneytracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneytracker.data.local.dao.BudgetDao
import com.moneytracker.data.local.dao.CategoryBudgetDao
import com.moneytracker.data.local.dao.CategoryDao
import com.moneytracker.data.local.dao.ExpenseDao
import com.moneytracker.data.local.dao.IncomeDao
import com.moneytracker.data.local.entity.BudgetEntity
import com.moneytracker.data.local.entity.CategoryBudgetEntity
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.ExpenseEntity
import com.moneytracker.data.local.entity.IncomeEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@Database(
    entities = [
        ExpenseEntity::class,
        IncomeEntity::class,
        BudgetEntity::class,
        CategoryEntity::class,
        CategoryBudgetEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao

    class Callback @Inject constructor(
        @ApplicationContext private val context: Context,
        private val categoryDao: Provider<CategoryDao>,
        private val budgetDao: Provider<BudgetDao>
    ) : RoomDatabase.Callback() {

        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            applicationScope.launch {
                seedCategories()
                seedBudget()
            }
        }

        private suspend fun seedCategories() {
            val categories = listOf(
                CategoryEntity(1, "Food", "Restaurant", 0xFFFF5A08.toInt()),
                CategoryEntity(2, "Coffee", "Coffee", 0xFFFF9800.toInt()),
                CategoryEntity(3, "Shopping", "ShoppingCart", 0xFFFF5722.toInt()),
                CategoryEntity(4, "Transportation", "DirectionsCar", 0xFF2196F3.toInt()),
                CategoryEntity(5, "Bills", "Receipt", 0xFF9C27B0.toInt()),
                CategoryEntity(6, "Entertainment", "Movie", 0xFFE91E63.toInt()),
                CategoryEntity(7, "Health", "LocalHospital", 0xFF4CAF50.toInt()),
                CategoryEntity(8, "Education", "School", 0xFF3F51B5.toInt()),
                CategoryEntity(9, "Gifts", "CardGiftcard", 0xFF00BCD4.toInt()),
                CategoryEntity(10, "Other", "MoreVert", 0xFF607D8B.toInt())
            )
            categoryDao.get().insertAll(categories)
        }

        private suspend fun seedBudget() {
            budgetDao.get().insert(
                BudgetEntity(monthlyBudget = 0.0, dailyBudget = 0.0, currency = "USD")
            )
        }
    }
}
