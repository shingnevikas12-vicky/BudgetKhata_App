package com.budgetkhata.ui.students

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetkhata.R
import com.budgetkhata.core.Dates
import com.budgetkhata.core.Money
import com.budgetkhata.core.PaymentStatus
import com.budgetkhata.core.PhoneUtils
import com.budgetkhata.core.Validation
import com.budgetkhata.data.local.entity.EventEntity
import com.budgetkhata.data.local.model.EnrollmentRow
import com.budgetkhata.data.local.model.PaymentRow
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.LocalAppSettings
import com.budgetkhata.ui.LocalSnackbarHostState
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.*
import com.budgetkhata.ui.payments.EditContributionDialog
import com.budgetkhata.ui.payments.LocalPaymentActions
import com.budgetkhata.ui.payments.RecordPaymentDialog
import com.budgetkhata.ui.theme.LocalStatusPalette

@Composable
fun StudentDetailScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenEvent: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val viewModel = appViewModel(container) { app, handle -> StudentDetailViewModel(app, handle) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = LocalAppSettings.current
    val actions = LocalPaymentActions.current
    val snackbar = LocalSnackbarHostState.current
    val context = LocalContext.current
    val palette = LocalStatusPalette.current

    var payingRow by remember { mutableStateOf<EnrollmentRow?>(null) }
    var addEventOpen by remember { mutableStateOf(false) }
    var contributionRow by remember { mutableStateOf<EnrollmentRow?>(null) }
    var paymentToDelete by remember { mutableStateOf<PaymentRow?>(null) }
    var deleteStudentOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbar?.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    val student = state.student

    Scaffold(
        topBar = {
            AppTopBar(
                title = student?.name ?: stringResource(R.string.student_profile),
                onBack = onBack,
                actions = {
                    if (student != null) {
                        IconButton(onClick = { onEdit(student.id) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_student))
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_actions))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_student)) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        deleteStudentOpen = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            LoadingBox(modifier = Modifier.padding(padding))
        } else if (student == null) {
            EmptyState(
                title = stringResource(R.string.student_not_found),
                message = stringResource(R.string.student_deleted_msg),
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Student Identity Header
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials(student.name),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                                Text(
                                    text = PhoneUtils.display(student.phone),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${student.phone}".toUri()))
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            ) {
                                Icon(Icons.Rounded.Call, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }

                // 2. Financial Summary Surface
                item {
                    val status = PaymentStatus.of(state.requiredPaise, state.paidPaise)
                    val containerColor = when (status) {
                        PaymentStatus.PAID -> palette.paid.copy(alpha = 0.1f)
                        PaymentStatus.PARTIAL -> palette.partial.copy(alpha = 0.1f)
                        else -> palette.pending.copy(alpha = 0.1f)
                    }
                    val contentColor = when (status) {
                        PaymentStatus.PAID -> palette.paid
                        PaymentStatus.PARTIAL -> palette.partial
                        else -> palette.pending
                    }

                    Surface(
                        color = containerColor,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(status.labelRes),
                                style = MaterialTheme.typography.titleMedium,
                                color = contentColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Money.format(state.pendingPaise),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = contentColor,
                                fontSize = 42.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.total_contribution), style = MaterialTheme.typography.labelMedium)
                                    Text(Money.format(state.requiredPaise), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.confirmed_paid), style = MaterialTheme.typography.labelMedium)
                                    Text(Money.format(state.paidPaise), fontWeight = FontWeight.Bold, color = palette.paid, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }

                // 3. Events Section
                item {
                    SectionHeader(
                        title = stringResource(R.string.events),
                        trailing = {
                            TextButton(onClick = { addEventOpen = true }) {
                                Icon(Icons.Rounded.GroupAdd, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(R.string.add), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                if (state.enrollments.isEmpty()) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.not_in_any_event),
                            message = stringResource(R.string.add_to_event_msg),
                            actionLabel = stringResource(R.string.add_to_event),
                            onAction = { addEventOpen = true }
                        )
                    }
                } else {
                    items(state.enrollments, key = { it.enrollmentId }) { row ->
                        DetailActionCard(
                            row = row,
                            palette = palette,
                            onRecord = { payingRow = row },
                            onRemind = { actions?.remind(row) },
                            onOpenEvent = { onOpenEvent(row.eventId) }
                        )
                    }
                }

                // 4. Payment Records
                if (state.payments.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.payment_history),
                            subtitle = stringResource(R.string.confirmed_by_you)
                        )
                    }
                    items(state.payments, key = { it.id }) { payment ->
                        HistoryTile(payment = payment, palette = palette) {
                            paymentToDelete = payment
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }

    // Dialogs
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
                ) {
                    payingRow = null
                }
            }
        )
    }

    if (addEventOpen) {
        AddToEventDialog(
            events = viewModel.events,
            enrolledEventIds = state.enrollments.map { it.eventId }.toSet(),
            errorMessage = viewModel.dialogError,
            onDismiss = {
                addEventOpen = false
                viewModel.clearDialogError()
            },
            onSave = { eventId, amount ->
                viewModel.addToEvent(eventId, amount) { addEventOpen = false }
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
            onSave = { amount ->
                viewModel.updateContribution(row, amount) { contributionRow = null }
            }
        )
    }

    paymentToDelete?.let { payment ->
        ConfirmDialog(
            title = stringResource(R.string.delete) + "?",
            message = stringResource(R.string.delete_payment_msg, Money.format(payment.amountPaise), Dates.format(payment.paidEpochDay)),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                viewModel.deletePayment(payment.id)
                paymentToDelete = null
            },
            onDismiss = { paymentToDelete = null }
        )
    }

    if (deleteStudentOpen && student != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_student_title, student.name),
            message = stringResource(R.string.delete_student_confirm_msg),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                deleteStudentOpen = false
                viewModel.deleteStudent(onDeleted)
            },
            onDismiss = { deleteStudentOpen = false }
        )
    }
}

@Composable
private fun DetailActionCard(
    row: EnrollmentRow,
    palette: com.budgetkhata.ui.theme.StatusPalette,
    onRecord: () -> Unit,
    onRemind: () -> Unit,
    onOpenEvent: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenEvent),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = row.eventName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = row.eventType.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusChip(row.status)
            }

            Spacer(Modifier.height(16.dp))

            val isPending = row.remainingPaise > 0L
            val color = when (row.status) {
                PaymentStatus.PAID -> palette.paid
                PaymentStatus.PARTIAL -> palette.partial
                else -> palette.pending
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.pending_amount) + ":", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = Money.format(row.remainingPaise),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }

            if (isPending) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onRecord,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.paid)
                ) {
                    Icon(Icons.Rounded.Payments, contentDescription = null)
                    Spacer(Modifier.size(12.dp))
                    Text(stringResource(R.string.action_collect_payment), fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRemind,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.whatsapp_reminder), fontSize = 16.sp)
                }
            } else {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = palette.paid.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.fully_paid),
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                        color = palette.paid,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTile(
    payment: PaymentRow,
    palette: com.budgetkhata.ui.theme.StatusPalette,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Money.format(payment.amountPaise),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.paid
                )
                Text(
                    text = "${payment.eventName} \u00B7 ${Dates.format(payment.paidEpochDay)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun AddToEventDialog(
    events: List<EventEntity>,
    enrolledEventIds: Set<Long>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Unit
) {
    val available = events.filter { it.id !in enrolledEventIds }
    var selectedId by remember { mutableStateOf(available.firstOrNull()?.id) }
    var amount by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_event), fontWeight = FontWeight.Bold, fontSize = 22.sp) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (available.isEmpty()) {
                    Text(stringResource(R.string.already_enrolled_msg), fontSize = 18.sp)
                } else {
                    EventSelector(
                        events = available,
                        selectedEventId = selectedId,
                        onSelect = { selectedId = it }
                    )
                    MoneyTextField(
                        value = amount,
                        onValueChange = {
                            amount = it
                            localError = null
                        },
                        label = stringResource(R.string.required_contribution),
                        isError = localError != null,
                        errorText = localError
                    )
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = available.isNotEmpty(),
                onClick = {
                    val paise = Money.parseToPaise(amount)
                    val error = Validation.contribution(paise)
                    if (paise == null || error != null) {
                        localError = error ?: R.string.valid_amount_error
                        return@Button
                    }
                    selectedId?.let { onSave(it, paise) }
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(R.string.add), fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), fontSize = 16.sp) }
        }
    )
}

private fun initials(name: String): String = name
    .split(" ")
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .take(1)
    .joinToString("")
    .ifBlank { "?" }
