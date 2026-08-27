/**
 * Settings / preferences screen.
 *
 * Sections (top-to-bottom):
 *  - Currency picker (USD / EUR / EGP / SAR / AED chips)
 *  - Theme picker (Light / Dark / System)
 *  - Language picker (System / English / Arabic)
 *  - Daily-summary notification toggle
 *  - Category Budgets (single picker form + list of existing budgets)
 *  - Backup & Restore (CSV export / import)
 *  - Reset all data (destructive, behind an AlertDialog)
 *
 * Long-press / destructive actions always go through an explicit
 * confirmation dialog so a stray tap can't wipe the user's data.
 */
package com.moneytracker.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.moneytracker.R
import com.moneytracker.domain.model.Category
import com.moneytracker.domain.model.CategoryBudget
import com.moneytracker.ui.components.formatCurrency
import com.moneytracker.util.LocaleHelper
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currency by viewModel.currency.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val locale by viewModel.locale.collectAsState()
    val dailySummaryEnabled by viewModel.dailySummaryEnabled.collectAsState()
    val categoryBudgets by viewModel.categoryBudgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val messageRes = when (event) {
                is SettingsViewModel.SettingsEvent.ShowMessage -> event.messageRes
                is SettingsViewModel.SettingsEvent.ShowError -> event.messageRes
            }
            Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportToCsv(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFromCsv(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.settings_currency), style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("USD", "EUR", "EGP", "SAR", "AED").forEach { code ->
                    FilterChip(
                        selected = currency == code,
                        onClick = { viewModel.setCurrency(code) },
                        label = { Text(code) }
                    )
                }
            }

            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val themeOptions = listOf(
                    "Light" to stringResource(R.string.settings_theme_light),
                    "Dark" to stringResource(R.string.settings_theme_dark),
                    "System" to stringResource(R.string.settings_theme_system)
                )
                themeOptions.forEach { (value, label) ->
                    FilterChip(
                        selected = theme == value,
                        onClick = { viewModel.setTheme(value) },
                        label = { Text(label) }
                    )
                }
            }

            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = listOf(
                    LocaleHelper.SYSTEM to stringResource(R.string.settings_language_system),
                    LocaleHelper.ENGLISH to stringResource(R.string.settings_language_english),
                    LocaleHelper.ARABIC to stringResource(R.string.settings_language_arabic)
                )
                options.forEach { (tag, label) ->
                    FilterChip(
                        selected = locale == tag,
                        onClick = { viewModel.setLocale(tag) },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_daily_summary))
                    Text(
                        text = stringResource(R.string.settings_daily_summary_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = dailySummaryEnabled,
                    onCheckedChange = { viewModel.setDailySummaryEnabled(it) }
                )
            }

            Text(stringResource(R.string.settings_category_budgets), style = MaterialTheme.typography.headlineSmall)
            CategoryBudgetsList(
                budgets = categoryBudgets,
                categories = categories,
                currency = currency,
                onSetBudget = { id, limit -> viewModel.setCategoryBudget(id, limit, currency) },
                onClear = { viewModel.clearCategoryBudget(it) }
            )

            Text(stringResource(R.string.settings_backup), style = MaterialTheme.typography.headlineSmall)
            Button(
                onClick = { exportLauncher.launch("money_tracker_backup.csv") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_export_csv))
            }
            Button(
                onClick = { importLauncher.launch(arrayOf("text/csv", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_import_csv))
            }

            Text(stringResource(R.string.settings_data), style = MaterialTheme.typography.headlineSmall)
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.settings_reset_all))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
            text = { Text(stringResource(R.string.settings_reset_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAll()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.settings_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.settings_reset_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBudgetsList(
    budgets: List<CategoryBudget>,
    categories: List<Category>,
    currency: String,
    onSetBudget: (Int, Double) -> Unit,
    onClear: (Int) -> Unit
) {
    val selectableCategories = categories.filter { it.name != "Other" }
    // Seed the picker with the first available category so the dropdown isn't blank.
    var selectedCategory by remember(selectableCategories) {
        mutableStateOf(selectableCategories.firstOrNull())
    }
    // Pre-fill the amount field with the existing budget when its category is picked.
    var amountText by remember(budgets, selectedCategory) {
        mutableStateOf(
            selectedCategory
                ?.let { cat -> budgets.firstOrNull { it.categoryId == cat.id }?.monthlyLimit }
                ?.toString()
                .orEmpty()
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Existing budgets — list once, not per-category rows.
        if (budgets.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_category_budgets_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                budgets.forEach { budget ->
                    val category = selectableCategories.firstOrNull { it.id == budget.categoryId }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = category?.name ?: stringResource(R.string.add_expense_other),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatCurrency(budget.monthlyLimit, budget.currency.ifBlank { currency }),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = {
                            // Re-select this category so the picker form pre-fills
                            // with its current amount for quick editing.
                            category?.let { selectedCategory = it }
                            amountText = budget.monthlyLimit.toString()
                        }) {
                            Text(stringResource(R.string.add_expense_save))
                        }
                        TextButton(onClick = { onClear(budget.categoryId) }) {
                            Text(stringResource(R.string.category_budget_clear))
                        }
                    }
                }
            }
        }

        // Single picker: choose a category, type a monthly limit, save.
        Text(
            text = stringResource(R.string.category_budget_set),
            style = MaterialTheme.typography.bodyMedium
        )
        var menuExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = menuExpanded,
            onExpandedChange = { menuExpanded = !menuExpanded }
        ) {
            OutlinedTextField(
                value = selectedCategory?.name.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.add_expense_category)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                selectableCategories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            selectedCategory = category
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    amountText = newValue
                }
            },
            label = { Text(stringResource(R.string.category_budget_set)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val cat = selectedCategory ?: return@Button
                val v = amountText.toDoubleOrNull()
                if (v != null && v > 0) {
                    onSetBudget(cat.id, v)
                    amountText = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.add_expense_save))
        }
    }
}
