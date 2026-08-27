/**
 * Full searchable transaction history.
 *
 * Renders every expense (and income) in a scrollable list with a search
 * box at the top that filters by description or category name (see
 * [com.moneytracker.data.local.dao.ExpenseDao.search]). Tapping a row
 * navigates to the edit screen.
 */
package com.moneytracker.ui.screens.expenseslist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.R
import com.moneytracker.domain.model.Transaction
import com.moneytracker.domain.model.isExpense
import com.moneytracker.ui.components.TransactionItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ExpensesListScreen(
    onNavigateBack: () -> Unit,
    onEditExpense: (Long) -> Unit,
    viewModel: ExpensesListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val categories = remember(uiState.transactions) {
        uiState.transactions
            .filterIsInstance<Transaction.ExpenseTransaction>()
            .map { it.category }
            .distinctBy { it.id }
    }

    val months = remember(uiState.transactions) {
        uiState.transactions
            .map { it.date.toString().substring(0, 7) }
            .distinct()
            .sortedDescending()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transactions_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text(stringResource(R.string.transactions_search)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.showExpenses,
                    onClick = { viewModel.toggleExpenses() },
                    label = { Text(stringResource(R.string.transactions_filter_expenses)) }
                )
                FilterChip(
                    selected = uiState.showIncome,
                    onClick = { viewModel.toggleIncome() },
                    label = { Text(stringResource(R.string.transactions_filter_income)) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedCategoryId == null,
                    onClick = { viewModel.setCategory(null) },
                    label = { Text(stringResource(R.string.transactions_filter_all_categories)) }
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategoryId == category.id,
                        onClick = { viewModel.setCategory(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedMonth == null,
                    onClick = { viewModel.setMonth(null) },
                    label = { Text(stringResource(R.string.transactions_filter_all_months)) }
                )
                months.forEach { month ->
                    FilterChip(
                        selected = uiState.selectedMonth == month,
                        onClick = { viewModel.setMonth(month) },
                        label = { Text(month) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.transactions,
                    key = { "${it.isExpense()}_${it.id}" }
                ) { transaction ->
                    val messageText = stringResource(R.string.transactions_deleted)
                    val actionText = stringResource(R.string.transactions_undo)
                    val dismissState = rememberDismissState(
                        confirmStateChange = { value ->
                            if (value == DismissValue.DismissedToStart) {
                                viewModel.deleteTransaction(transaction)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = messageText,
                                        actionLabel = actionText
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreLastDeleted()
                                    }
                                }
                                true
                            } else {
                                false
                            }
                        }
                    )
                    SwipeToDismiss(
                        state = dismissState,
                        directions = setOf(DismissDirection.EndToStart),
                        background = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.transactions_delete),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        dismissContent = {
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
                    )
                }
            }
        }
    }
}
