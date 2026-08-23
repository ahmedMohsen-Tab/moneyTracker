package com.moneytracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moneytracker.R
import com.moneytracker.domain.model.Transaction
import com.moneytracker.domain.model.displaySign
import com.moneytracker.domain.model.isExpense
import com.moneytracker.domain.model.isIncome

@Composable
fun TransactionItem(
    transaction: Transaction,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (transaction) {
        is Transaction.ExpenseTransaction -> transaction.category.name
        is Transaction.IncomeTransaction -> stringResource(R.string.transactions_income_title)
    }
    val description = transaction.description.ifBlank { stringResource(R.string.transactions_no_description) }
    val icon = when (transaction) {
        is Transaction.ExpenseTransaction -> categoryIcon(transaction.category.iconName)
        is Transaction.IncomeTransaction -> Icons.Default.AccessTime
    }
    val tint = when (transaction) {
        is Transaction.ExpenseTransaction -> Color(transaction.category.color)
        is Transaction.IncomeTransaction -> MaterialTheme.colorScheme.primary
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${transaction.date} • ${transaction.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = "${transaction.displaySign()}${formatCurrency(transaction.amount, currency)}",
                style = MaterialTheme.typography.headlineSmall,
                color = when {
                    transaction.isExpense() -> MaterialTheme.colorScheme.error
                    transaction.isIncome() -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun categoryIcon(iconName: String): ImageVector = when (iconName) {
    "Restaurant" -> Icons.Default.Restaurant
    "Coffee" -> Icons.Default.LocalCafe
    "ShoppingCart" -> Icons.Default.ShoppingCart
    "DirectionsCar" -> Icons.Default.DirectionsCar
    "Receipt" -> Icons.Default.Receipt
    "Movie" -> Icons.Default.Movie
    "LocalHospital" -> Icons.Default.LocalHospital
    "School" -> Icons.Default.School
    "CardGiftcard" -> Icons.Default.CardGiftcard
    else -> Icons.Default.MoreVert
}
