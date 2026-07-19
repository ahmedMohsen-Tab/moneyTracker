package com.moneytracker.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.domain.model.Transaction
import com.moneytracker.ui.components.BalanceCard
import com.moneytracker.ui.components.SummaryCard
import com.moneytracker.ui.components.TransactionItem
import com.moneytracker.ui.components.greeting

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.Today

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddExpense: () -> Unit,
    onViewAll: () -> Unit,
    onEditExpense: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(greeting()) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(
                            targetState = selectedMonth,
                            label = "month_animation"
                        ) { month ->
                            Text(
                                text = month.toString(),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        if (selectedMonth != java.time.YearMonth.now()) {
                            IconButton(onClick = { viewModel.resetToCurrentMonth() }) {
                                Icon(Icons.Default.Today, contentDescription = "Current Month")
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }
            }
            item {
                BalanceCard(
                    balance = uiState.balance,
                    currency = uiState.currency
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        title = "Bank",
                        amount = uiState.bankBalance,
                        currency = uiState.currency,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Cash",
                        amount = uiState.cashBalance,
                        currency = uiState.currency,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Card",
                        amount = uiState.creditCardBalance,
                        currency = uiState.currency,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        title = "Spent Today",
                        amount = uiState.spentToday,
                        currency = uiState.currency,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Spent This Month",
                        amount = uiState.spentThisMonth,
                        currency = uiState.currency,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Remaining",
                        amount = uiState.remaining,
                        currency = uiState.currency,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (uiState.budgetUsage >= 0.8) {
                item {
                    val message = when {
                        uiState.budgetUsage >= 1.0 -> "You have exceeded your monthly budget!"
                        uiState.budgetUsage >= 0.9 -> "You have used 90% of your monthly budget."
                        else -> "You have used 80% of your monthly budget."
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.budgetUsage >= 1.0) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    TextButton(onClick = onViewAll) {
                        Text("View All")
                    }
                }
            }
            items(uiState.recentTransactions) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    currency = uiState.currency,
                    onClick = {
                        if (transaction is Transaction.ExpenseTransaction) {
                            onEditExpense(transaction.id)
                        }
                    }
                )
            }
        }
    }
}
