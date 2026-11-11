package com.budgetkhata.ui.expenses

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.budgetkhata.R
import com.budgetkhata.data.local.entity.ExpenseCategory
import com.budgetkhata.di.AppContainer
import com.budgetkhata.ui.appViewModel
import com.budgetkhata.ui.components.AppTextField
import com.budgetkhata.ui.components.AppTopBar
import com.budgetkhata.ui.components.DateField
import com.budgetkhata.ui.components.ErrorBanner
import com.budgetkhata.ui.components.EventSelector
import com.budgetkhata.ui.components.LoadingBox
import com.budgetkhata.ui.components.MoneyTextField
import com.budgetkhata.ui.components.OptionDropdown
import com.budgetkhata.ui.components.SectionHeader

@Composable
fun ExpenseEditScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel = appViewModel(container) { app, handle -> ExpenseEditViewModel(app, handle) }
    val form = viewModel.form
    val context = LocalContext.current

    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (error: SecurityException) {
                // Some providers do not support persistable grants; the image is still usable this session.
            }
            viewModel.updateReceipt(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (viewModel.isNew) stringResource(R.string.add_expense) else stringResource(R.string.edit) + " " + stringResource(R.string.expenses),
                onBack = onBack
            )
        }
    ) { padding ->
        if (viewModel.loading) {
            LoadingBox(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            viewModel.message?.let { message ->
                ErrorBanner(message = message, onRetry = viewModel::dismissMessage)
            }

            SectionHeader(
                title = stringResource(R.string.expenses) + " " + stringResource(R.string.settings),
                subtitle = stringResource(R.string.track_expenses_msg)
            )

            EventSelector(
                events = viewModel.events,
                selectedEventId = form.eventId,
                onSelect = viewModel::updateEvent,
                enabled = viewModel.events.isNotEmpty(),
                label = stringResource(R.string.event)
            )
            form.eventError?.let {
                Text(stringResource(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }

            AppTextField(
                value = form.title,
                onValueChange = viewModel::updateTitle,
                label = stringResource(R.string.description_optional),
                placeholder = "Bus booking advance",
                isError = form.titleError != null,
                errorText = form.titleError
            )

            MoneyTextField(
                value = form.amount,
                onValueChange = viewModel::updateAmount,
                label = stringResource(R.string.amount_paid),
                isError = form.amountError != null,
                errorText = form.amountError
            )

            DateField(label = stringResource(R.string.payment_date), epochDay = form.epochDay, onPick = viewModel::updateDate)

            OptionDropdown(
                label = stringResource(R.string.event_type), // Or category
                options = ExpenseCategory.entries.toList(),
                selected = form.category,
                labelOf = { context.getString(it.labelRes) },
                onSelect = viewModel::updateCategory
            )

            AppTextField(
                value = form.notes,
                onValueChange = viewModel::updateNotes,
                label = stringResource(R.string.notes_optional),
                singleLine = false,
                maxLines = 3
            )

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedButton(onClick = { receiptPicker.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Rounded.AttachFile, contentDescription = null)
                    Text("  Attach receipt")
                }
                if (!form.receiptUri.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.receipt_attached),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                    IconButton(onClick = { viewModel.updateReceipt(null) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                    }
                }
            }

            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = !viewModel.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.saving) stringResource(R.string.loading) else stringResource(R.string.save) + " " + stringResource(R.string.expenses))
            }

            TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
        }
    }
}
