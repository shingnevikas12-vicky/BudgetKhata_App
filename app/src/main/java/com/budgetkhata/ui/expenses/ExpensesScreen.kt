package com.budgetkhata.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetkhata.R
import com.budgetkhata.core.Dates
import com.budgetkhata.core.Money
import com.budgetkhata.data.local.model.ExpenseRow
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.LocalSnackbarHostState
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.ConfirmDialog
import com.budgetkhata.ui.components.EmptyState
import com.budgetkhata.ui.components.LoadingBox
import com.budgetkhata.ui.components.SectionHeader
import com.budgetkhata.ui.theme.LocalStatusPalette

@Composable
fun ExpensesScreen(
    container: AppContainer,
    onAddExpense: (Long?) -> Unit,
    onEditExpense: (Long) -> Unit,
    onOpenReports: () -> Unit
) {
    val viewModel = appViewModel(container) { app, handle -> ExpensesViewModel(app, handle) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val palette = LocalStatusPalette.current
    var deleteTarget by remember { mutableStateOf<ExpenseRow?>(null) }

    LaunchedEffect(viewModel.message) {
        val message = viewModel.message
        if (message != null) {
            snackbar?.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.expenses),
                actions = {
                    IconButton(onClick = onOpenReports) {
                        Icon(Icons.Rounded.Assessment, contentDescription = stringResource(R.string.reports))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddExpense(state.eventFilter) },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_expense)) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${state.expenses.size} " + stringResource(R.string.expenses),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Net balance appears in each event summary",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = Money.format(state.totalPaise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.expense
                    )
                }
            }

            if (state.events.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = state.eventFilter == null,
                                onClick = { viewModel.setFilter(null) },
                                label = { Text(stringResource(R.string.all_events)) }
                            )
                        }
                        items(state.events, key = { it.id }) { event ->
                            FilterChip(
                                selected = state.eventFilter == event.id,
                                onClick = {
                                    viewModel.setFilter(if (state.eventFilter == event.id) null else event.id)
                                },
                                label = { Text(event.name, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            if (state.loading) {
                item { LoadingBox() }
            } else if (state.expenses.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.no_expenses_recorded),
                        message = stringResource(R.string.track_expenses_msg),
                        icon = Icons.Rounded.ReceiptLong,
                        actionLabel = stringResource(R.string.add_expense),
                        onAction = { onAddExpense(state.eventFilter) }
                    )
                }
            } else {
                state.grouped.forEach { (eventName, eventExpenses) ->
                    item(key = "header-$eventName") {
                        SectionHeader(
                            title = eventName,
                            subtitle = "${Money.format(eventExpenses.sumOf { it.amountPaise })} across ${eventExpenses.size} entries"
                        )
                    }
                    items(eventExpenses, key = { it.id }) { expense ->
                        ExpenseCard(
                            expense = expense,
                            onEdit = { onEditExpense(expense.id) },
                            onDelete = { deleteTarget = expense }
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { expense ->
        ConfirmDialog(
            title = stringResource(R.string.delete) + "?",
            message = "${expense.title} \u00B7 ${Money.format(expense.amountPaise)} will be removed and the event net balance recalculated.",
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                viewModel.delete(expense.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun ExpenseCard(expense: ExpenseRow, onEdit: () -> Unit, onDelete: () -> Unit) {
    val palette = LocalStatusPalette.current
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${expense.category.label} \u00B7 ${Dates.format(expense.spentEpochDay)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!expense.receiptUri.isNullOrBlank()) {
                    Text(
                        text = "Receipt attached",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (!expense.notes.isNullOrBlank()) {
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = Money.format(expense.amountPaise),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = palette.expense
            )
            Spacer(Modifier.size(4.dp))
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}
