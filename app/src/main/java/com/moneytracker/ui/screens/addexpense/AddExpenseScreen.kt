package com.moneytracker.ui.screens.addexpense

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.R
import com.moneytracker.ui.components.AmountInput
import com.moneytracker.ui.components.CategoryDropdown
import com.moneytracker.ui.components.DatePickerField
import com.moneytracker.ui.components.TimePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    expenseId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(expenseId) {
        if (expenseId != null && expenseId > 0) {
            viewModel.loadExpense(expenseId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddExpenseViewModel.AddExpenseEvent.NavigateBack -> onNavigateBack()
                is AddExpenseViewModel.AddExpenseEvent.ShowError -> {
                    val message = context.getString(event.messageRes)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            uiState.isEdit && uiState.isIncome -> stringResource(R.string.edit_income_title)
                            uiState.isEdit -> stringResource(R.string.edit_expense_title)
                            uiState.isIncome -> stringResource(R.string.add_income_title)
                            else -> stringResource(R.string.add_expense_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
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
            if (!uiState.isEdit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleIncome(false) },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isIncome
                    ) {
                        Text(stringResource(R.string.add_expense_toggle_expense))
                    }
                    Button(
                        onClick = { viewModel.toggleIncome(true) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isIncome
                    ) {
                        Text(stringResource(R.string.add_expense_toggle_income))
                    }
                }
            }

            AmountInput(
                value = uiState.amount,
                onValueChange = { viewModel.updateAmount(it) }
            )

            if (!uiState.isIncome) {
                CategoryDropdown(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { viewModel.updateCategory(it) },
                    onOtherSelected = { viewModel.onOtherCategorySelected() }
                )
            }

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text(stringResource(R.string.add_expense_description)) },
                modifier = Modifier.fillMaxWidth()
            )

            DatePickerField(
                date = uiState.date,
                onDateSelected = { viewModel.updateDate(it) }
            )

            TimePickerField(
                time = uiState.time,
                onTimeSelected = { viewModel.updateTime(it) }
            )

            Text(stringResource(R.string.add_expense_wallet), style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.selectableGroup()) {
                listOf("Cash", "Bank").forEach { wallet ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = uiState.wallet == wallet,
                                onClick = { viewModel.updateWallet(wallet) },
                                role = Role.RadioButton
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.wallet == wallet,
                            onClick = null
                        )
                        Text(wallet, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.add_expense_cancel))
                }
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.add_expense_save))
                }
            }
        }

        if (uiState.showNewCategoryDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissNewCategoryDialog() },
                title = { Text(stringResource(R.string.add_expense_new_category)) },
                text = {
                    OutlinedTextField(
                        value = uiState.newCategoryName,
                        onValueChange = { viewModel.updateNewCategoryName(it) },
                        label = { Text(stringResource(R.string.add_expense_new_category_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmNewCategory() }) {
                        Text(stringResource(R.string.add_expense_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissNewCategoryDialog() }) {
                        Text(stringResource(R.string.add_expense_cancel))
                    }
                }
            )
        }
    }
}
