package com.budgetkhata.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.budgetkhata.core.PaymentStatus
import com.budgetkhata.core.PhoneUtils
import com.budgetkhata.core.Validation
import com.budgetkhata.data.local.entity.StudentEntity
import com.budgetkhata.data.local.model.EnrollmentRow
import com.budgetkhata.data.local.model.EventSummary
import com.budgetkhata.data.local.model.ExpenseRow
import com.budgetkhata.data.local.model.PaymentRow
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.LocalAppSettings
import com.budgetkhata.ui.LocalSnackbarHostState
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AmountRow
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.ConfirmDialog
import com.budgetkhata.ui.components.EmptyState
import com.budgetkhata.ui.components.InfoBanner
import com.budgetkhata.ui.components.LoadingBox
import com.budgetkhata.ui.components.MoneyTextField
import com.budgetkhata.ui.components.ProgressBar
import com.budgetkhata.ui.components.SearchField
import com.budgetkhata.ui.components.SectionHeader
import com.budgetkhata.ui.components.StatusChip
import com.budgetkhata.ui.payments.EditContributionDialog
import com.budgetkhata.ui.payments.LocalPaymentActions
import com.budgetkhata.ui.payments.RecordPaymentDialog
import com.budgetkhata.ui.theme.LocalStatusPalette

@Composable
fun EventDetailScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onEditEvent: (Long) -> Unit,
    onOpenStudent: (Long) -> Unit,
    onAddExpense: (Long) -> Unit,
    onEditExpense: (Long) -> Unit,
    onOpenReports: (Long) -> Unit
) {
    val viewModel = appViewModel(container) { app, handle -> EventDetailViewModel(app, handle) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val settings = LocalAppSettings.current
    val actions = LocalPaymentActions.current
    val snackbar = LocalSnackbarHostState.current
    val palette = LocalStatusPalette.current

    val tabs = listOf(
        stringResource(R.string.students),
        stringResource(R.string.payments),
        stringResource(R.string.expenses),
        stringResource(R.string.summary)
    )

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var payingRow by remember { mutableStateOf<EnrollmentRow?>(null) }
    var contributionRow by remember { mutableStateOf<EnrollmentRow?>(null) }
    var removeRow by remember { mutableStateOf<EnrollmentRow?>(null) }
    var paymentToDelete by remember { mutableStateOf<PaymentRow?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseRow?>(null) }
    var addStudentsOpen by remember { mutableStateOf(false) }
    var deleteEventOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.message) {
        val message = viewModel.message
        if (message != null) {
            snackbar?.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    val event = state.event

    Scaffold(
        topBar = {
            AppTopBar(
                title = event?.name ?: stringResource(R.string.event),
                onBack = onBack,
                actions = {
                    if (event != null) {
                        IconButton(onClick = { onEditEvent(event.id) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_event))
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.event_actions))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.reports_for_this_event)) },
                                    leadingIcon = { Icon(Icons.Rounded.Assessment, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        onOpenReports(event.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (event.isArchived) stringResource(R.string.restore_event) else stringResource(R.string.archive_event)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (event.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.setArchived(!event.isArchived)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_event)) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        deleteEventOpen = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (event != null) {
                when (tab) {
                    0 -> ExtendedFloatingActionButton(
                        onClick = { addStudentsOpen = true },
                        icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                        text = { Text(stringResource(R.string.add_students)) }
                    )

                    2 -> ExtendedFloatingActionButton(
                        onClick = { onAddExpense(event.id) },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.add_expense)) }
                    )
                }
            }
        }
    ) { padding ->
        when {
            state.loading -> LoadingBox(modifier = Modifier.padding(padding))

            event == null -> EmptyState(
                title = stringResource(R.string.event_not_found),
                message = stringResource(R.string.event_deleted_msg),
                modifier = Modifier.padding(padding)
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                state.summary?.let { summary -> EventSummaryHeader(summary = summary, palette = palette) }

                TabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
                            text = { Text(label) }
                        )
                    }
                }

                when (tab) {
                    0 -> StudentsTab(
                        enrollments = state.enrollments,
                        onOpenStudent = onOpenStudent,
                        onRecordPayment = { payingRow = it },
                        onRemind = { actions?.remind(it) },
                        onShareQr = { actions?.shareQr(it) },
                        onPayUpi = { actions?.payWithUpi(it) },
                        onEditContribution = { contributionRow = it },
                        onRemove = { removeRow = it },
                        onAddStudents = { addStudentsOpen = true }
                    )

                    1 -> PaymentsTab(
                        payments = state.payments,
                        onDelete = { paymentToDelete = it }
                    )

                    2 -> ExpensesTab(
                        expenses = state.expenses,
                        onAdd = { onAddExpense(event.id) },
                        onEdit = onEditExpense,
                        onDelete = { expenseToDelete = it }
                    )

                    else -> SummaryTab(
                        summary = state.summary,
                        onOpenReports = { onOpenReports(event.id) }
                    )
                }
            }
        }
    }

    payingRow?.let { row ->
        RecordPaymentDialog(
            studentName = row.studentName,
            eventName = row.eventName,
            remainingPaise = row.remainingPaise,
            allowOverpayment = settings.allowOverpayment,
            saving = viewModel.saving,
            errorMessage = viewModel.dialogError,
            onDismiss = {
                payingRow = null
                viewModel.clearDialogError()
            },
            onSave = { amount, epochDay, method, reference, notes ->
                viewModel.recordPayment(
                    enrollment = row,
                    amountPaise = amount,
                    epochDay = epochDay,
                    method = method,
                    reference = reference,
                    notes = notes,
                    allowOverpayment = settings.allowOverpayment
                ) { payingRow = null }
            }
        )
    }

    contributionRow?.let { row ->
        EditContributionDialog(
            row = row,
            errorMessage = viewModel.dialogError,
            onDismiss = {
                contributionRow = null
                viewModel.clearDialogError()
            },
            onSave = { amount -> viewModel.updateContribution(row, amount) { contributionRow = null } }
        )
    }

    if (addStudentsOpen) {
        EnrollStudentsDialog(
            students = students,
            enrolledIds = state.enrollments.map { it.studentId }.toSet(),
            errorMessage = viewModel.dialogError,
            onDismiss = {
                addStudentsOpen = false
                viewModel.clearDialogError()
            },
            onEnroll = { ids, amount ->
                viewModel.enrollStudents(ids, amount) { addStudentsOpen = false }
            }
        )
    }

    removeRow?.let { row ->
        ConfirmDialog(
            title = stringResource(R.string.remove_from_event) + "?",
            message = stringResource(R.string.delete_student_msg, row.paymentCount), // Reusing similar message
            confirmLabel = stringResource(R.string.remove),
            destructive = true,
            onConfirm = {
                viewModel.removeEnrollment(row.enrollmentId)
                removeRow = null
            },
            onDismiss = { removeRow = null }
        )
    }

    paymentToDelete?.let { payment ->
        ConfirmDialog(
            title = stringResource(R.string.delete) + "?",
            message = "${Money.format(payment.amountPaise)} from ${payment.studentName} on ${Dates.format(payment.paidEpochDay)}",
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                viewModel.deletePayment(payment.id)
                paymentToDelete = null
            },
            onDismiss = { paymentToDelete = null }
        )
    }

    expenseToDelete?.let { expense ->
        ConfirmDialog(
            title = stringResource(R.string.delete) + "?",
            message = "${expense.title} \u00B7 ${Money.format(expense.amountPaise)} on ${Dates.format(expense.spentEpochDay)}",
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                viewModel.deleteExpense(expense.id)
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }

    if (deleteEventOpen && event != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_event_title, event.name),
            message = stringResource(R.string.delete_event_msg, state.enrollments.size, Money.format(state.summary?.collectedPaise ?: 0L)),
            confirmLabel = stringResource(R.string.delete_event),
            destructive = true,
            onConfirm = {
                deleteEventOpen = false
                viewModel.deleteEvent(onBack)
            },
            onDismiss = { deleteEventOpen = false }
        )
    }
}

@Composable
private fun EventSummaryHeader(
    summary: EventSummary,
    palette: com.budgetkhata.ui.theme.StatusPalette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = summary.eventType.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${Money.format(summary.collectedPaise)} of ${Money.format(summary.expectedPaise)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${summary.studentCount} " + stringResource(R.string.students),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${Money.format(summary.pendingPaise)} " + stringResource(R.string.pending),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.pendingPaise > 0L) palette.pending else palette.paid
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(progress = summary.collectionPercent / 100f, color = palette.paid)
    }
}

@Composable
private fun StudentsTab(
    enrollments: List<EnrollmentRow>,
    onOpenStudent: (Long) -> Unit,
    onRecordPayment: (EnrollmentRow) -> Unit,
    onRemind: (EnrollmentRow) -> Unit,
    onShareQr: (EnrollmentRow) -> Unit,
    onPayUpi: (EnrollmentRow) -> Unit,
    onEditContribution: (EnrollmentRow) -> Unit,
    onRemove: (EnrollmentRow) -> Unit,
    onAddStudents: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            InfoBanner(
                text = stringResource(R.string.record_payment_banner)
            )
        }

        if (enrollments.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.no_students_in_event),
                    message = stringResource(R.string.add_students_msg),
                    icon = Icons.Rounded.PersonAdd,
                    actionLabel = stringResource(R.string.add_students),
                    onAction = onAddStudents
                )
            }
        } else {
            items(enrollments, key = { it.enrollmentId }) { row ->
                EventEnrollmentCard(
                    row = row,
                    onOpenStudent = { onOpenStudent(row.studentId) },
                    onRecordPayment = { onRecordPayment(row) },
                    onRemind = { onRemind(row) },
                    onShareQr = { onShareQr(row) },
                    onPayUpi = { onPayUpi(row) },
                    onEditContribution = { onEditContribution(row) },
                    onRemove = { onRemove(row) }
                )
            }
        }
    }
}

@Composable
private fun EventEnrollmentCard(
    row: EnrollmentRow,
    onOpenStudent: () -> Unit,
    onRecordPayment: () -> Unit,
    onRemind: () -> Unit,
    onShareQr: () -> Unit,
    onPayUpi: () -> Unit,
    onEditContribution: () -> Unit,
    onRemove: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val palette = LocalStatusPalette.current

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = row.studentName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenStudent)
                ) {
                    Text(
                        text = row.studentName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = PhoneUtils.display(row.studentPhone),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(row.status)
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.student_actions))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.open_profile)) },
                            onClick = {
                                menuOpen = false
                                onOpenStudent()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_contribution)) },
                            onClick = {
                                menuOpen = false
                                onEditContribution()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_from_event)) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onRemove()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.total_contribution), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Money.format(row.requiredPaise), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.amount_paid), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Money.format(row.paidPaise), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.paid)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.pending_amount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Money.format(row.remainingPaise),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (row.remainingPaise > 0L) palette.pending else palette.paid
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            ProgressBar(
                progress = row.progress,
                color = when (row.status) {
                    PaymentStatus.PAID -> palette.paid
                    PaymentStatus.PARTIAL -> palette.partial
                    PaymentStatus.PENDING -> palette.pending
                }
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                TextButton(onClick = onRecordPayment, enabled = row.remainingPaise > 0L) {
                    Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(if (row.status == PaymentStatus.PAID) stringResource(R.string.paid) else stringResource(R.string.record))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRemind, enabled = row.remainingPaise > 0L) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = stringResource(R.string.whatsapp_reminder), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onShareQr) {
                    Icon(Icons.Rounded.QrCode2, contentDescription = stringResource(R.string.share_qr), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onPayUpi, enabled = row.remainingPaise > 0L) {
                    Icon(Icons.Rounded.Payments, contentDescription = stringResource(R.string.open_upi), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun PaymentsTab(payments: List<PaymentRow>, onDelete: (PaymentRow) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            InfoBanner(stringResource(R.string.payments_confirmed_banner))
        }

        if (payments.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.no_payments_yet),
                    message = stringResource(R.string.record_payment_msg),
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong
                )
            }
        } else {
            items(payments, key = { it.id }) { payment ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = Money.format(payment.amountPaise),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalStatusPalette.current.paid
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = payment.method.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${payment.studentName} \u00B7 ${Dates.format(payment.paidEpochDay)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val reference = listOfNotNull(
                                payment.reference?.takeIf { it.isNotBlank() },
                                payment.notes?.takeIf { it.isNotBlank() }
                            ).joinToString(" \u00B7 ")
                            if (reference.isNotEmpty()) {
                                Text(
                                    text = reference,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = { onDelete(payment) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpensesTab(
    expenses: List<ExpenseRow>,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (ExpenseRow) -> Unit
) {
    val palette = LocalStatusPalette.current
    val total = expenses.sumOf { it.amountPaise }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.total_expenses), style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = Money.format(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.expense
                    )
                }
                TextButton(onClick = onAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.add))
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.no_expenses_recorded),
                    message = stringResource(R.string.track_expenses_msg),
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    actionLabel = stringResource(R.string.add_expense),
                    onAction = onAdd
                )
            }
        } else {
            items(expenses, key = { it.id }) { expense ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Money.format(expense.amountPaise),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.expense
                            )
                            Row {
                                IconButton(
                                    onClick = { onEdit(expense.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(17.dp))
                                }
                                IconButton(
                                    onClick = { onDelete(expense) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTab(summary: EventSummary?, onOpenReports: () -> Unit) {
    val palette = LocalStatusPalette.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (summary == null) {
            item { LoadingBox() }
        } else {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(stringResource(R.string.financial_summary), summary.eventName)
                        Spacer(Modifier.height(8.dp))
                        AmountRow(stringResource(R.string.expected), Money.format(summary.expectedPaise), emphasized = true)
                        AmountRow(stringResource(R.string.collected), Money.format(summary.collectedPaise), valueColor = palette.paid, emphasized = true)
                        AmountRow(stringResource(R.string.pending), Money.format(summary.pendingPaise), valueColor = palette.pending, emphasized = true)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        AmountRow(stringResource(R.string.expenses), Money.format(summary.expensePaise), valueColor = palette.expense, emphasized = true)
                        AmountRow(
                            stringResource(R.string.net_balance),
                            Money.format(summary.netPaise),
                            valueColor = if (summary.netPaise >= 0L) palette.income else palette.expense,
                            emphasized = true
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.net_balance_msg),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(stringResource(R.string.payments), stringResource(R.string.payment_status_across, summary.studentCount))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(PaymentStatus.PAID)
                            Text("${summary.paidCount}")
                            Spacer(Modifier.size(8.dp))
                            StatusChip(PaymentStatus.PENDING)
                            Text("${summary.pendingCount}")
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.collection_percent_msg, summary.collectionPercent.toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                ElevatedCard(onClick = onOpenReports, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Assessment, contentDescription = null)
                        Spacer(Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.reports_for_this_event), style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Paid, partial and pending lists, event summary and expense report as CSV or PDF",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                InfoBanner(
                    text = "Only confirmed payments are counted. UPI links, generated QR codes and WhatsApp " +
                        "messages are sharing tools, not proof of payment."
                )
            }
        }
    }
}

@Composable
private fun EnrollStudentsDialog(
    students: List<StudentEntity>,
    enrolledIds: Set<Long>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onEnroll: (List<Long>, Long) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var amount by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<Int?>(null) }

    val available = students.filter { it.id !in enrolledIds }
    val filtered = available.filter { student ->
        query.isBlank() ||
            student.name.contains(query, ignoreCase = true) ||
            student.phone.contains(query)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_students)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (available.isEmpty()) {
                    Text(
                        text = "Every student is already enrolled. Create a new student from the Students tab first.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    SearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = stringResource(R.string.search)
                    )

                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(filtered, key = { it.id }) { student ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected = if (student.id in selected) {
                                            selected - student.id
                                        } else {
                                            selected + student.id
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = student.id in selected,
                                    onCheckedChange = { checked ->
                                        selected = if (checked) selected + student.id else selected - student.id
                                    }
                                )
                                Column {
                                    Text(student.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        PhoneUtils.display(student.phone),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    MoneyTextField(
                        value = amount,
                        onValueChange = {
                            amount = it
                            localError = null
                        },
                        label = "Contribution for selected students",
                        isError = localError != null,
                        errorText = localError,
                        supportingText = "${selected.size} student(s) selected"
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected.isNotEmpty() && available.isNotEmpty(),
                onClick = {
                    val paise = Money.parseToPaise(amount)
                    val error = Validation.contribution(paise)
                    if (paise == null || error != null) {
                        localError = error ?: R.string.valid_amount_error
                        return@TextButton
                    }
                    onEnroll(selected.toList(), paise)
                }
            ) { Text(stringResource(R.string.add) + " ${selected.size}") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
