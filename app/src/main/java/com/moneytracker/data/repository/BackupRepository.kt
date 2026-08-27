package com.moneytracker.data.repository

import android.content.Context
import android.net.Uri
import com.moneytracker.data.backup.CsvCodec
import com.moneytracker.data.local.dao.ExpenseDao
import com.moneytracker.data.local.dao.IncomeDao
import com.moneytracker.data.local.entity.ExpenseEntity
import com.moneytracker.data.local.entity.IncomeEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns CSV export and import so the settings screen is no longer a swiss-army knife.
 * Pure I/O — never touches the rest of the app's settings.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val incomeDao: IncomeDao,
    @ApplicationContext private val context: Context
) {

    /**
     * @return number of rows written (expenses + incomes).
     */
    suspend fun exportToCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(uri) ?: return@withContext 0
        var rowCount = 0
        output.use { stream ->
            OutputStreamWriter(stream).use { writer ->
                writer.appendLine("type,id,amount,categoryId,description,date,time,wallet")
                expenseDao.getAll().first().forEach { withCategory ->
                    val it = withCategory.expense
                    writer.appendLine(
                        "expense,${it.id},${it.amount},${it.categoryId}," +
                            "\"${CsvCodec.escape(it.description)}\",${it.date},${it.time},${it.wallet}"
                    )
                    rowCount++
                }
                incomeDao.getAll().first().forEach {
                    writer.appendLine(
                        "income,${it.id},${it.amount},,\"${CsvCodec.escape(it.description)}\",${it.date},${it.time},${it.wallet}"
                    )
                    rowCount++
                }
            }
        }
        rowCount
    }

    /**
     * @return number of rows imported.
     */
    suspend fun importFromCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: return@withContext 0
        var imported = 0
        input.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                val lines = reader.readLines()
                if (lines.isEmpty()) return@use
                // Drop the header row.
                lines.drop(1).forEach { line ->
                    val parts = CsvCodec.parseLine(line)
                    if (parts.isEmpty()) return@forEach
                    when (parts[0]) {
                        "expense" -> if (parts.size >= 8) {
                            expenseDao.insert(
                                ExpenseEntity(
                                    id = 0,
                                    amount = parts[2].toDoubleOrNull() ?: 0.0,
                                    categoryId = parts[3].toIntOrNull() ?: 10,
                                    description = parts.getOrNull(4).orEmpty(),
                                    date = parts.getOrNull(5).orEmpty(),
                                    time = parts.getOrNull(6).orEmpty(),
                                    timestamp = System.currentTimeMillis(),
                                    wallet = parts.getOrNull(7).orEmpty().ifBlank { "Cash" }
                                )
                            )
                            imported++
                        }
                        "income" -> if (parts.size >= 8) {
                            incomeDao.insert(
                                IncomeEntity(
                                    id = 0,
                                    amount = parts[2].toDoubleOrNull() ?: 0.0,
                                    description = parts.getOrNull(4).orEmpty(),
                                    date = parts.getOrNull(5).orEmpty(),
                                    time = parts.getOrNull(6).orEmpty(),
                                    timestamp = System.currentTimeMillis(),
                                    wallet = parts.getOrNull(7).orEmpty().ifBlank { "Cash" }
                                )
                            )
                            imported++
                        }
                    }
                }
            }
        }
        imported
    }
}
