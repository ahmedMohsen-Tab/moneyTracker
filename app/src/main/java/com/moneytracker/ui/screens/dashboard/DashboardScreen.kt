/**
 * The home screen.
 *
 * Renders (inside a LazyColumn):
 *  - Month navigation row (prev / current / next)
 *  - BalanceCard (total balance in chosen currency)
 *  - Wallet summary row (Bank + Cash)
 *  - Today spent / This month spent / Remaining row
 *  - Recent transactions list (top 10)
 *  - Optional Category Budgets section (top 3 by usage %)
 *
 * A `DisposableEffect` listens for `ON_RESUME` lifecycle events and
 * calls `viewModel.onResume()` so the "today" boundary refreshes when
 * the user backgrounds and re-foregrounds the app across midnight.
 */
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.R
import com.moneytracker.domain.model.Transaction
import com.moneytracker.domain.model.stableKey
import com.moneytracker.ui.components.BalanceCard
import com.moneytracker.ui.components.SummaryCard
import com.moneytracker.ui.components.TransactionItem
import com.moneytracker.ui.components.greeting

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Color
import com.moneytracker.domain.model.CategoryBudgetUsage
import com.moneytracker.domain.model.CategoryBudgetStatus
import com.moneytracker.ui.components.formatCurrency

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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.dashboard_previous_month))
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
                                Icon(Icons.Default.Today, contentDescription = stringResource(R.string.dashboard_current_month))
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.dashboard_next_month))
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
                        title = stringResource(R.string.dashboard_wallet_bank),
                        amount = uiState.bankBalance,
                        currency = uiState.currency,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = stringResource(R.string.dashboard_wallet_cash),
                        amount = uiState.cashBalance,
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
                        title = stringResource(R.string.dashboard_spent_today),
                        amount = uiState.spentToday,
                        currency = uiState.currency,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = stringResource(R.string.dashboard_spent_this_month),
                        amount = uiState.spentThisMonth,
                        currency = uiState.currency,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = stringResource(R.string.dashboard_remaining),
                        amount = uiState.remaining,
                        currency = uiState.currency,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (uiState.budgetUsage >= 0.8) {
                item {
                    val message = when {
                        uiState.budgetUsage >= 1.0 -> stringResource(R.string.dashboard_warning_100)
                        uiState.budgetUsage >= 0.9 -> stringResource(R.string.dashboard_warning_90)
                        else -> stringResource(R.string.dashboard_warning_80)
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
                        text = stringResource(R.string.dashboard_recent_transactions),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    TextButton(onClick = onViewAll) {
                        Text(stringResource(R.string.dashboard_view_all))
                    }
                }
            }
            if (uiState.categoryBudgetUsages.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.dashboard_category_budgets),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        uiState.categoryBudgetUsages.take(3).forEach { usage ->
                            CategoryBudgetRow(usage = usage)
                        }
                    }
                }
            }
            // Stable key per transaction so LazyColumn can reuse composition
            // slots when the list reorders (e.g., a new expense pushing the
            // others down) instead of tearing down every visible row.
            //
            // Must be type-qualified (see [Transaction.stableKey]) because
            // expenses and incomes live in separate Room tables with their
            // own `autoGenerate` id sequences — they are allowed to share
            // the same numeric id, which previously produced
            // `IllegalArgumentException: Key "1" was already used` and
            // crashed the dashboard on first render after the very first
            // income + expense were inserted.
            items(uiState.recentTransactions, key = { transaction -> transaction.stableKey() }) { transaction ->
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

@Composable
private fun CategoryBudgetRow(usage: CategoryBudgetUsage) {
    val color = when (usage.status) {
        CategoryBudgetStatus.OVER_LIMIT -> MaterialTheme.colorScheme.error
        CategoryBudgetStatus.NEAR_LIMIT -> MaterialTheme.colorScheme.error
        CategoryBudgetStatus.WARNING -> MaterialTheme.colorScheme.tertiary
        CategoryBudgetStatus.OK -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(usage.category.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${formatCurrency(usage.spent, usage.currency)} / ${formatCurrency(usage.limit, usage.currency)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { usage.percent.coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = color
            )
        }
    }
}
