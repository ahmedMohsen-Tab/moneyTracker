package com.moneytracker.ui.screens.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.ui.components.BarChartView
import com.moneytracker.ui.components.LineChartView
import com.moneytracker.ui.components.PieChartView
import com.moneytracker.ui.components.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Monthly Summary",
                style = MaterialTheme.typography.headlineSmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryItem(
                    title = "Total Income",
                    amount = uiState.totalIncome,
                    currency = uiState.currency,
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    title = "Total Expenses",
                    amount = uiState.totalExpenses,
                    currency = uiState.currency,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryItem(
                    title = "Remaining",
                    amount = uiState.remainingBalance,
                    currency = uiState.currency,
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    title = "Avg Daily",
                    amount = uiState.averageDailySpending,
                    currency = uiState.currency,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryItem(
                    title = "Transactions",
                    amount = uiState.transactionCount.toDouble(),
                    currency = "",
                    modifier = Modifier.weight(1f),
                    isCount = true
                )
                SummaryItem(
                    title = "Highest Day",
                    amount = uiState.highestSpendingDay?.second ?: 0.0,
                    currency = uiState.currency,
                    modifier = Modifier.weight(1f)
                )
            }
            uiState.highestSpendingCategory?.let { (name, amount) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Highest Category",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$name: ${formatCurrency(amount, uiState.currency)}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Monthly Spending Trend",
                style = MaterialTheme.typography.headlineSmall
            )
            LineChartView(
                data = uiState.dailySpending.toList().sortedBy { it.first },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.headlineSmall
            )
            PieChartView(
                data = uiState.categorySpending,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Weekly Spending",
                style = MaterialTheme.typography.headlineSmall
            )
            BarChartView(
                data = uiState.weeklySpending,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SummaryItem(
    title: String,
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier,
    isCount: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isCount) "${amount.toInt()}" else formatCurrency(amount, currency),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
